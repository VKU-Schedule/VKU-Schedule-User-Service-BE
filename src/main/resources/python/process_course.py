"""
Data Processing Script for Course Information.

This script reads course information from a specific sheet in an Excel file,
cleans the data, and outputs it into two formats:
1. A cleaned CSV file.
2. A structured JSON file, organized by academic year, semester, cohort, and class.

Usage:
    Run the script from the root directory of the project.

    python ingest_data/process_course_data.py --input path/to/your/excel_file.xlsx

Example:
    python ingest_data/process_course_data.py --input "ingest_data/raw/Danh mục các học phần giảng dạy_HKI_25-26.xlsx"
"""

import argparse
import json
import os
import re
from typing import Any, Dict, List, Optional, Tuple

import pandas as pd


def extract_metadata_from_title(title: str) -> Tuple[str, int]:
    """
    Extract academic year and semester number from the title string.

    Example (case-insensitive):
        "... HỌC KỲ 1 ... NĂM HỌC 2025-2026 ..."
    Returns:
        (academic_year_str, semester_number_int)
    """
    m_year = re.search(r"NĂM\s*HỌC\s+(\d{4}-\d{4})", title, flags=re.IGNORECASE)
    academic_year = m_year.group(1) if m_year else "UNKNOWN"

    m_sem = re.search(r"HỌC\s*KỲ\s+(\d+)", title, flags=re.IGNORECASE)
    semester_no = int(m_sem.group(1)) if m_sem else 0

    return academic_year, semester_no


def clean_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """
    Promote the 5th row as header, drop the first 5 rows, and normalize column names.

    Expected original order after promoting header:
        [ten_hp, lt, th, tong, ghi_chu, lop, ...]
    Returns:
        DataFrame with English columns:
        ['course_name', 'theory_credits', 'practical_credits', 'total_credits', 'subtopic', 'class']
    """
    # Promote row 5 (index 4) as header
    df.columns = df.iloc[4]
    # Drop top 5 rows
    df = df.iloc[5:].reset_index(drop=True)

    # Drop "STT" if present
    if "STT" in df.columns:
        df = df.drop(columns=["STT"])

    cols = list(df.columns)
    if len(cols) < 6:
        raise ValueError(
            "Input sheet does not contain the expected 6+ columns after cleaning."
        )

    # Map to canonical English names (credits-based)
    cols[0] = "course_name"  # tên học phần
    cols[1] = "theory_credits"  # LT: số tín chỉ lý thuyết
    cols[2] = "practical_credits"  # TH: số tín chỉ thực hành
    cols[3] = "total_credits"  # Tổng số tín chỉ
    cols[4] = "subtopic"  # Chủ đề phụ
    cols[5] = "class"  # Lớp

    df.columns = cols
    return df


def expand_class_token(token: str) -> List[str]:
    """
    Expand a class token to a list of class names.

    Supported patterns:
        "19SE1->SE5" -> ["19SE1", "19SE2", "19SE3", "19SE4", "19SE5"]
        "22SE1->2"   -> ["22SE1", "22SE2"]
    """
    token = token.strip()
    if "->" not in token:
        return [token] if token else []

    left, right = token.split("->", maxsplit=1)
    left = left.strip()
    right = right.strip()

    m_left = re.match(r"([A-Za-z0-9_]*?)(\d+)$", left)
    if not m_left:
        return [token]

    left_prefix, start_str = m_left.groups()
    start = int(start_str)

    if right.isdigit():
        end = int(right)
    else:
        m_right = re.match(r"([A-Za-z0-9_]*?)(\d+)$", right)
        if not m_right:
            return [token]
        right_prefix, end_str = m_right.groups()
        end = int(end_str)

        # Only check suffix compatibility
        if not left_prefix.endswith(right_prefix):
            return [token]

    if end < start:
        return [token]

    return [f"{left_prefix}{i}" for i in range(start, end + 1)]


def get_cohort_and_classes(token: str) -> Tuple[Optional[int], List[str]]:
    """
    Expand a class token and return (cohort, expanded_classes).

    Cohort is inferred as the first integer prefix in the first expanded class.
    Example: "19SE1->SE5" -> cohort 19, classes ["19SE1", ..., "19SE5"]
    """
    classes = expand_class_token(token)
    cohort: Optional[int] = None

    if classes:
        m = re.match(r"(\d+)", classes[0])
        if m:
            cohort = int(m.group(1))

    return cohort, classes


def safe_float(x: Any) -> float:
    """Convert to float if possible; return 0.0 on NaN/None/invalid."""
    try:
        if pd.isna(x):
            return 0.0
        return float(x)
    except Exception:
        return 0.0


def clean_course_name(course_name: str) -> str:
    """
    Clean course name by removing content in parentheses.
    
    Example:
        "Thực tập doanh nghiệp (IT)" -> "Thực tập doanh nghiệp"
        "Lập trình game" -> "Lập trình game"
    
    Returns:
        cleaned_course_name
    """
    import re
    
    # Remove the parentheses part from course name
    cleaned_name = re.sub(r'\s*\([^)]+\)', '', course_name).strip()
    return cleaned_name


def build_result(
    df: pd.DataFrame, academic_year: str, semester_no: int
) -> Dict[str, Any]:
    """
    Build the normalized JSON structure:

    {
        "academic_year": "2025-2026",
        "semesters": {
            "semester_1": {
                19: {
                    "19SE1": [
                        {
                          "course_name": "...",
                          "theory_credits": 3.0,
                          "practical_credits": 1.0,
                          "total_credits": 4.0,
                          "subtopic": "..."
                        },
                        ...
                    ],
                    ...
                },
                ...
            }
        }
    }
    """
    result: Dict[str, Any] = {"academic_year": academic_year, "semesters": {}}
    semester_key = f"semester_{semester_no}" if semester_no > 0 else "semester_unknown"
    result["semesters"][semester_key] = {}

    for _, row in df.iterrows():
        raw = str(row.get("class", "")).strip()
        if not raw or raw.lower() == "nan":
            continue

        tokens = [t.strip() for t in raw.split(",") if t.strip()]
        for token in tokens:
            cohort, classes = get_cohort_and_classes(token)
            if not classes:
                continue

            cohort_key = cohort if cohort is not None else 0
            if cohort_key not in result["semesters"][semester_key]:
                result["semesters"][semester_key][cohort_key] = {}

            class_map = result["semesters"][semester_key][cohort_key]
            for class_name in classes:
                if class_name not in class_map:
                    class_map[class_name] = []

                # Clean course name by removing parentheses like (IT)
                raw_course_name = str(row.get("course_name", "")).strip()
                cleaned_name = clean_course_name(raw_course_name)
                
                class_map[class_name].append(
                    {
                        "course_name": cleaned_name,
                        "theory_credits": safe_float(row.get("theory_credits")),
                        "practical_credits": safe_float(row.get("practical_credits")),
                        "total_credits": safe_float(row.get("total_credits")),
                        "subtopic": (
                            None
                            if pd.isna(row.get("subtopic"))
                            else str(row.get("subtopic")).strip()
                        ),
                    }
                )

    return result


def ensure_parent_dir(path: str) -> None:
    """Create parent directory of the given path if it does not exist."""
    parent = os.path.dirname(os.path.abspath(path))
    if parent and not os.path.exists(parent):
        os.makedirs(parent, exist_ok=True)


def run(
    input_path: str,
    sheet_name: str,
    csv_output: str,
    json_output: str,
) -> None:
    """Main execution pipeline."""
    df_raw = pd.read_excel(input_path, sheet_name=sheet_name)

    # Title assumed to be the first column header cell of the raw frame
    original_title = str(df_raw.columns[0])
    academic_year, semester_no = extract_metadata_from_title(original_title)

    # Clean dataframe (promote headers & normalize column names)
    df_clean = clean_dataframe(df_raw)

    # Persist cleaned CSV
    ensure_parent_dir(csv_output)
    df_clean.to_csv(csv_output, index=False, encoding="utf-8-sig")

    # Build normalized JSON
    result = build_result(df_clean, academic_year, semester_no)

    # Save JSON
    ensure_parent_dir(json_output)
    with open(json_output, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=4, ensure_ascii=False)

    # Console feedback
    print(f"Academic year extracted: {academic_year}")
    print(f"Semester extracted: {semester_no if semester_no else 'UNKNOWN'}")
    print(f"Cleaned CSV saved to: {csv_output}")
    print(f"JSON saved to: {json_output}")


def parse_args() -> argparse.Namespace:
    """Parse CLI arguments."""
    parser = argparse.ArgumentParser(
        description="Convert Excel course list to cleaned CSV and normalized JSON."
    )
    parser.add_argument(
        "--input",
        required=True,
        help="Path to the Excel file.",
    )
    parser.add_argument(
        "--sheet",
        default="Sheet1",
        help="Worksheet name to read (default: Sheet1).",
    )
    parser.add_argument(
        "--csv-output",
        default="ingest_data/cleaned/cleaned_data_courses.csv",
        help="Output path for cleaned CSV.",
    )
    parser.add_argument(
        "--json-output",
        default="ingest_data/cleaned/courses_by_class.json",
        help="Output path for normalized JSON.",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    # Use json_output from csv_output parameter for compatibility with Java service
    json_out = args.csv_output if args.csv_output else args.json_output
    run(
        input_path=args.input,
        sheet_name=args.sheet,
        csv_output=args.csv_output,
        json_output=json_out,
    )