package com.example.user_service.controller;

import com.example.user_service.entity.*;
import com.example.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CohortRepository cohortRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final com.example.user_service.service.UserScheduleService userScheduleService;

    @GetMapping("/academic-years")
    public ResponseEntity<List<AcademicYear>> getAcademicYears() {
        return ResponseEntity.ok(academicYearRepository.findAll());
    }

    @GetMapping("/semesters")
    public ResponseEntity<List<Semester>> getSemesters(@RequestParam Long academicYearId) {
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
            .orElseThrow(() -> new RuntimeException("Academic year not found"));
        return ResponseEntity.ok(semesterRepository.findByAcademicYear(academicYear));
    }

    @GetMapping("/cohorts")
    public ResponseEntity<List<Cohort>> getCohorts(@RequestParam Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new RuntimeException("Semester not found"));
        return ResponseEntity.ok(cohortRepository.findBySemester(semester));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<ClassEntity>> getClasses(@RequestParam Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
            .orElseThrow(() -> new RuntimeException("Cohort not found"));
        return ResponseEntity.ok(classRepository.findByCohort(cohort));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getCourses(@RequestParam Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new RuntimeException("Class not found"));
        return ResponseEntity.ok(courseRepository.findByClassEntity(classEntity));
    }

    @GetMapping("/schedules/by-course")
    public ResponseEntity<List<Schedule>> getSchedulesByCourse(
            @RequestParam String courseName,
            @RequestParam(required = false) String subtopic) {
        List<Schedule> schedules;
        
        if (subtopic != null && !subtopic.isEmpty() && !"null".equalsIgnoreCase(subtopic)) {
            // Query for schedules with specific subtopic (not empty)
            schedules = scheduleRepository.findByCourseNameAndSubtopicNotEmpty(courseName, subtopic);
        } else {
            // Query for schedules with empty subtopic
            schedules = scheduleRepository.findByCourseNameAndSubtopicEmpty(courseName);
        }
        
        return ResponseEntity.ok(schedules);
    }

    @PostMapping("/schedules/save")
    public ResponseEntity<?> saveSchedule(@RequestBody com.example.user_service.dto.SaveScheduleRequest request) {
        try {
            UserSchedule saved = userScheduleService.saveSchedule(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save schedule: " + e.getMessage());
        }
    }

    @GetMapping("/schedules/my-schedules")
    public ResponseEntity<List<UserSchedule>> getMySchedules(
            @RequestParam Long userId,
            @RequestParam(required = false) Long semesterId) {
        try {
            List<UserSchedule> schedules = userScheduleService.getUserSchedules(userId, semesterId);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<String> deleteSchedule(@PathVariable Long scheduleId) {
        try {
            userScheduleService.deleteSchedule(scheduleId);
            return ResponseEntity.ok("Schedule deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete schedule: " + e.getMessage());
        }
    }
}
