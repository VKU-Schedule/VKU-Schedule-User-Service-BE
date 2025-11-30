package com.example.user_service.service;

import com.example.user_service.entity.Schedule;
import com.example.user_service.entity.Semester;
import com.example.user_service.repository.ScheduleRepository;
import com.example.user_service.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class ScheduleImportService {

    private final PythonScriptService pythonScriptService;
    private final ScheduleRepository scheduleRepository;
    private final SemesterRepository semesterRepository;

    @Transactional
    public int importSchedule(MultipartFile file) throws Exception {
        // Get original file extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".xlsx";
        
        // Save uploaded file temporarily
        Path tempInput = Files.createTempFile("schedule_input_", extension);
        Files.copy(file.getInputStream(), tempInput, StandardCopyOption.REPLACE_EXISTING);

        // Process with Python script
        Path tempOutput = Files.createTempFile("schedule_output_", ".csv");
        pythonScriptService.executeScript("process_classes.py", 
            tempInput.toString(), 
            tempOutput.toString());

        // Delete all old schedule data
        scheduleRepository.deleteAll();

        // Read processed CSV and save to database
        int count = 0;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(tempOutput.toFile()), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, 
                CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreSurroundingSpaces()
                    .withTrim())) {
            
            for (CSVRecord record : csvParser) {
                Schedule schedule = new Schedule();
                schedule.setCourseName(getRecordValue(record, "Tên học phần"));
                schedule.setClassNumber(parseInteger(getRecordValue(record, "Lớp")));
                schedule.setLanguage(getRecordValue(record, "Ngôn ngữ"));
                schedule.setMajor(getRecordValue(record, "Chuyên ngành"));
                schedule.setClassGroup(getRecordValue(record, "Lớp theo học"));
                schedule.setSubtopic(getRecordValue(record, "Chủ đề phụ"));
                schedule.setInstructor(getRecordValue(record, "Giảng viên"));
                schedule.setDayOfWeek(getRecordValue(record, "Thứ"));
                schedule.setPeriods(getRecordValue(record, "Tiết"));
                schedule.setLocation(getRecordValue(record, "Khu vực"));
                schedule.setRoomNumber(getRecordValue(record, "Số phòng"));
                schedule.setWeeks(getRecordValue(record, "Tuần học"));
                schedule.setCapacity(parseInteger(getRecordValue(record, "Sỉ số")));
                
                scheduleRepository.save(schedule);
                count++;
            }
        }

        // Cleanup temp files
        Files.deleteIfExists(tempInput);
        Files.deleteIfExists(tempOutput);

        return count;
    }

    private String getRecordValue(CSVRecord record, String columnName) {
        try {
            // Try with BOM
            if (record.isMapped("\uFEFF" + columnName)) {
                return record.get("\uFEFF" + columnName);
            }
            // Try without BOM
            return record.get(columnName);
        } catch (IllegalArgumentException e) {
            // Column not found, return empty string
            return "";
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
