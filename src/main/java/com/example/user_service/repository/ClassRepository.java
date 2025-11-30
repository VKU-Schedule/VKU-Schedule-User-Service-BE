package com.example.user_service.repository;

import com.example.user_service.entity.ClassEntity;
import com.example.user_service.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    List<ClassEntity> findByCohort(Cohort cohort);
    Optional<ClassEntity> findByClassCodeAndCohort(String classCode, Cohort cohort);
}
