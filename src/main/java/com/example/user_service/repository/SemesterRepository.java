package com.example.user_service.repository;

import com.example.user_service.entity.Semester;
import com.example.user_service.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findByAcademicYear(AcademicYear academicYear);
    Optional<Semester> findBySemesterNameAndAcademicYear(String semesterName, AcademicYear academicYear);
}
