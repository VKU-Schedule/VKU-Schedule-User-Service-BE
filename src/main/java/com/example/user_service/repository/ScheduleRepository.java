package com.example.user_service.repository;

import com.example.user_service.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByCourseName(String courseName);
    List<Schedule> findByCourseNameContainingIgnoreCase(String courseName);
    
    // Query for schedules with specific subtopic (not empty)
    @Query("SELECT s FROM Schedule s WHERE s.courseName = :courseName AND s.subtopic = :subtopic AND s.subtopic != ''")
    List<Schedule> findByCourseNameAndSubtopicNotEmpty(
        @org.springframework.data.repository.query.Param("courseName") String courseName, 
        @org.springframework.data.repository.query.Param("subtopic") String subtopic
    );
    
    // Query for schedules with empty subtopic
    @Query("SELECT s FROM Schedule s WHERE s.courseName = :courseName AND (s.subtopic IS NULL OR s.subtopic = '')")
    List<Schedule> findByCourseNameAndSubtopicEmpty(
        @org.springframework.data.repository.query.Param("courseName") String courseName
    );
    
    @Query("SELECT COUNT(DISTINCT s.courseName) FROM Schedule s")
    long countDistinctCourseName();
}
