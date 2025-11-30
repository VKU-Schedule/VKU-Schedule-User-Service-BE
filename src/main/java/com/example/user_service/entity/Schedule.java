package com.example.user_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "class_number")
    private Integer classNumber;

    @Column(name = "language")
    private String language;

    @Column(name = "major")
    private String major;

    @Column(name = "class_group")
    private String classGroup;

    @Column(name = "subtopic")
    private String subtopic;

    @Column(name = "instructor")
    private String instructor;

    @Column(name = "day_of_week")
    private String dayOfWeek;

    @Column(name = "periods", columnDefinition = "TEXT")
    private String periods;

    @Column(name = "location")
    private String location;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "weeks", columnDefinition = "TEXT")
    private String weeks;

    @Column(name = "capacity")
    private Integer capacity;
}
