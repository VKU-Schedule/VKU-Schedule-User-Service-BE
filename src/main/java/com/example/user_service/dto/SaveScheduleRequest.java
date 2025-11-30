package com.example.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveScheduleRequest {
    private Long userId;
    private Long semesterId;
    private List<ScheduleDTO> schedules;
    private String parsedPrompt;
}
