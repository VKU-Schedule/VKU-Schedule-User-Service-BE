package com.example.user_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "semester_name", nullable = false)
    private String semesterName;
    
    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "schedule", columnDefinition = "JSON", nullable = false)
    private String schedule;

    @Column(name = "parsed_prompt", columnDefinition = "TEXT")
    private String parsedPrompt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
