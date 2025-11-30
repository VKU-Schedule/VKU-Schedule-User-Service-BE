package com.example.user_service.repository;

import com.example.user_service.entity.Cohort;
import com.example.user_service.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CohortRepository extends JpaRepository<Cohort, Long> {
    List<Cohort> findBySemester(Semester semester);
    Optional<Cohort> findByCohortCodeAndSemester(String cohortCode, Semester semester);
}
