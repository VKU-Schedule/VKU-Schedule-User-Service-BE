package com.example.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {
    private String courseName;
    private Integer classNumber;
    private String language;
    private String major;
    private String classGroup;
    private String subtopic;
    private String instructor;
    private String dayOfWeek;
    private String periods;
    private String location;
    private String roomNumber;
    private String weeks;
    private Integer capacity;
}
