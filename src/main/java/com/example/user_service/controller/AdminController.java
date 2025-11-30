package com.example.user_service.controller;

import com.example.user_service.dto.CourseImportResponse;
import com.example.user_service.dto.ScheduleImportResponse;
import com.example.user_service.entity.Schedule;
import com.example.user_service.repository.ScheduleRepository;
import com.example.user_service.service.CourseImportService;
import com.example.user_service.service.ScheduleImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ScheduleImportService scheduleImportService;
    private final CourseImportService courseImportService;
    private final ScheduleRepository scheduleRepository;

    @PostMapping("/schedules/import")
    public ResponseEntity<ScheduleImportResponse> importSchedule(@RequestParam("file") MultipartFile file) {
        try {
            int count = scheduleImportService.importSchedule(file);
            return ResponseEntity.ok(new ScheduleImportResponse(true, "Import successful", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ScheduleImportResponse(false, "Import failed: " + e.getMessage(), 0));
        }
    }

    @PostMapping("/courses/import")
    public ResponseEntity<CourseImportResponse> importCourses(@RequestParam("file") MultipartFile file) {
        try {
            int count = courseImportService.importCourses(file);
            return ResponseEntity.ok(new CourseImportResponse(true, "Import successful", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new CourseImportResponse(false, "Import failed: " + e.getMessage(), 0));
        }
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<Schedule>> getAllSchedules(
            @RequestParam(required = false) String courseName) {
        if (courseName != null && !courseName.isEmpty()) {
            return ResponseEntity.ok(scheduleRepository.findByCourseNameContainingIgnoreCase(courseName));
        }
        return ResponseEntity.ok(scheduleRepository.findAll());
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            long totalSchedules = scheduleRepository.count();
            long totalCourses = scheduleRepository.countDistinctCourseName();
            // For now, return 0 for students as we don't have user count yet
            long totalStudents = 0;
            
            return ResponseEntity.ok(new java.util.HashMap<String, Long>() {{
                put("totalSchedules", totalSchedules);
                put("totalCourses", totalCourses);
                put("totalStudents", totalStudents);
            }});
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get statistics: " + e.getMessage());
        }
    }
}
