package com.example.user_service.repository;

import com.example.user_service.entity.UserSchedule;
import com.example.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {
    List<UserSchedule> findByUserAndSemesterName(User user, String semesterName);
    List<UserSchedule> findByUser(User user);
}
