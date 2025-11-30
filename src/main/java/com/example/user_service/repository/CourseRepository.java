package com.example.user_service.repository;

import com.example.user_service.entity.Course;
import com.example.user_service.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByClassEntity(ClassEntity classEntity);
}
