import pandas as pd
import re
import os
import sys

# Add current directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from constant import VALID_MAJORS, INVALID_ROOM


import argparse

# Parse command line arguments
parser = argparse.ArgumentParser()
parser.add_argument('--input', required=True, help='Input file path')
parser.add_argument('--csv-output', required=True, help='Output CSV file path')
args = parser.parse_args()

# Đọc dữ liệu từ Excel/CSV
file_path = args.input
if file_path.endswith('.xlsx') or file_path.endswith('.xls'):
    df = pd.read_excel(file_path)
else:
    # Try different encodings
    try:
        df = pd.read_csv(file_path, encoding='utf-8')
    except UnicodeDecodeError:
        try:
            df = pd.read_csv(file_path, encoding='latin-1')
        except UnicodeDecodeError:
            df = pd.read_csv(file_path, encoding='cp1252')

# Kiểm tra xem dữ liệu có được đọc đúng không
if df.empty:
    raise ValueError("Dữ liệu không có trong file hoặc sheet không đúng.")


# Hàm xử lý cột "Tên lớp học phần"
def process_class_name(class_name):
    if pd.isna(class_name):
        return "", "", "", "", ""

    # Kiểm tra định dạng GDTC
    gdtc_match = re.match(r'GDTC\s*(\d+)\s*(?:\((.*?)\))?\s*(?:-(\d+))?', class_name)
    if gdtc_match:
        name = f'GDTC {gdtc_match.group(1)}'
        subtopic = gdtc_match.group(2) if gdtc_match.group(2) else 'Không có'
        class_num = gdtc_match.group(3) if gdtc_match.group(3) else ''
        return name.strip(), int(class_num) if class_num else "", "Tiếng Việt", "", subtopic

    # Xác định chuyên ngành
    major_match = re.search(r"\((\w+)\)", class_name)
    major = major_match.group(1) if major_match and major_match.group(1) in VALID_MAJORS else ""

    # Xác định số thứ tự lớp
    class_number_match = re.search(r"\(([-\d]+)\)", class_name)
    class_number = int(class_number_match.group(1).lstrip("-0")) if class_number_match else ""

    # Xác định ngôn ngữ
    language = "Tiếng Anh" if "_TA" in class_name else "Tiếng Việt"

    # Tách chủ đề phụ
    subtopic_match = re.search(r"_(?!TA)(.+)", class_name)
    subtopic = subtopic_match.group(1) if subtopic_match else ""

    # Lọc tên học phần chính
    name_cleaned = re.sub(r"\(.*?\)|_.*", "", class_name).strip()

    return name_cleaned, class_number, language, major, subtopic


# Áp dụng xử lý tên lớp học phần
df[["Tên học phần", "Lớp", "Ngôn ngữ", "Chuyên ngành", "Chủ đề phụ"]] = df["Tên lớp học phần"].apply(
    lambda x: pd.Series(process_class_name(str(x)))
)

def process_subtopic_level2(subtopic):
    if pd.isna(subtopic) or subtopic.strip() == "":
        return "", ""

    parts = subtopic.split("_")

    # Không có dấu "_" → kiểm tra xem có phải chữ viết tắt không
    if len(parts) == 1:
        value = parts[0].strip()
        # Nếu là chữ viết tắt (toàn chữ hoa, <=5 ký tự, không có khoảng trắng)
        # → đây là "Lớp theo học", chủ đề phụ để trống
        if value.isupper() and len(value) <= 5 and ' ' not in value:
            return value, ""
        # Nếu có ý nghĩa (có khoảng trắng hoặc dài hơn hoặc có chữ thường)
        # → đây là "Chủ đề phụ", lớp theo học để trống
        return "", value

    # Có 1 dấu "_" (2 phần): phần 1 là lớp theo học, phần 2 là chủ đề phụ
    # VD: "EF_Tiền số và công nghệ blockchain" → class_group="EF", subtopic="Tiền số và công nghệ blockchain"
    # VD: "GBA,BA_TA" → class_group="GBA, BA", subtopic="" (vì TA là viết tắt, không có ý nghĩa)
    if len(parts) == 2:
        class_group = parts[0].replace(",", ", ").strip()
        subtopic_part = parts[1].strip()
        
        # Nếu phần 2 là chữ viết tắt (toàn chữ hoa, <=3 ký tự), bỏ qua
        if subtopic_part.isupper() and len(subtopic_part) <= 3:
            return class_group, ""
        
        return class_group, subtopic_part

    # Có >=2 dấu "_" (>=3 phần): phần 1 là lớp theo học, phần 2 là chủ đề phụ, phần 3+ bỏ
    # VD: "GIT_UX thực tế_TA" → class_group="GIT", subtopic="UX thực tế" (bỏ "_TA")
    # VD: "SE_Java web_TA" → class_group="SE", subtopic="Java web" (bỏ "_TA")
    class_group = parts[0].replace(",", ", ").strip()
    subtopic_clean = parts[1].strip()
    
    # Nếu phần 2 là chữ viết tắt (toàn chữ hoa, <=3 ký tự), bỏ qua
    if subtopic_clean.isupper() and len(subtopic_clean) <= 3:
        return class_group, ""
    
    return class_group, subtopic_clean


# Áp dụng tách lần 2 vào DataFrame
df[["Lớp theo học", "Chủ đề phụ"]] = df["Chủ đề phụ"].apply(
    lambda x: pd.Series(process_subtopic_level2(str(x)))
)

# Hàm xử lý cột "Thời khóa biểu"
def process_schedule(schedule):
    if pd.isna(schedule):
        return "", ""
    schedule = str(schedule).strip()
    day_match = re.search(r"(Thứ \w+)", schedule)
    day = day_match.group(1) if day_match else ""
    period_match = re.search(r"Tiết ([\d,->]+)", schedule)
    periods = period_match.group(1) if period_match else ""
    period_list = []
    if periods:
        try:
            for part in periods.split(","):
                if "->" in part:
                    start, end = map(int, part.split("->"))
                    period_list.extend(range(start, end + 1))
                else:
                    period_list.append(int(part))
        except ValueError:
            period_list = []
    return day, period_list


# Áp dụng xử lý thời khóa biểu
df[["Thứ", "Tiết"]] = df["Thời khóa biểu"].apply(lambda x: pd.Series(process_schedule(str(x))))


# Hàm xử lý cột "Phòng học"
def process_room(room):
    if pd.isna(room):
        return "", ""
    room = str(room).strip()
    if room in INVALID_ROOM:
        return room, ""
    room_match = re.match(r"([A-Z])\.(.+)", room)
    if room_match:
        return room_match.group(1), room_match.group(2)
    return "Khác", room


# Áp dụng xử lý phòng học
df[["Khu vực", "Số phòng"]] = df["Phòng học"].apply(lambda x: pd.Series(process_room(str(x))))

# Đảm bảo tất cả các số độc lập hiển thị dưới dạng số
df["Lớp"] = pd.to_numeric(df["Lớp"], errors='coerce').astype('Int64')  # Giữ NaN thay vì chuỗi rỗng
df["Sỉ số"] = pd.to_numeric(df["Sỉ số"], errors='coerce').astype('Int64')  # Giữ NaN thay vì chuỗi rỗng

# Lưu dữ liệu đã xử lý vào file CSV
df_cleaned = df[[
    "Tên học phần", "Lớp", "Ngôn ngữ", "Chuyên ngành",
    "Lớp theo học", "Chủ đề phụ",
    "Giảng viên", "Thứ", "Tiết",
    "Khu vực", "Số phòng", "Tuần học", "Sỉ số"
]]

# Lưu vào file CSV (without BOM for Java compatibility)
df_cleaned.to_csv(args.csv_output, index=False, encoding='utf-8')
print(f"Dữ liệu đã được lưu vào file {args.csv_output}")
