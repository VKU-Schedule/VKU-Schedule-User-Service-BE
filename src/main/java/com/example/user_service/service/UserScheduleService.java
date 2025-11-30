package com.example.user_service.service;

import com.example.user_service.dto.SaveScheduleRequest;
import com.example.user_service.dto.ScheduleDTO;
import com.example.user_service.entity.Semester;
import com.example.user_service.entity.User;
import com.example.user_service.entity.UserSchedule;
import com.example.user_service.repository.SemesterRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.repository.UserScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserScheduleService {

    private final UserScheduleRepository userScheduleRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public UserSchedule saveSchedule(SaveScheduleRequest request) throws JsonProcessingException {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get semester info for display purposes only
        Semester semester = semesterRepository.findById(request.getSemesterId())
            .orElseThrow(() -> new RuntimeException("Semester not found"));

        // Convert schedules list to JSON string
        String scheduleJson = objectMapper.writeValueAsString(request.getSchedules());

        UserSchedule userSchedule = new UserSchedule();
        userSchedule.setUser(user);
        userSchedule.setSemesterName(semester.getSemesterName());
        userSchedule.setAcademicYear(semester.getAcademicYear().getYearName());
        userSchedule.setSchedule(scheduleJson);
        userSchedule.setPrompt(request.getPrompt());
        userSchedule.setParsedPrompt(request.getParsedPrompt());

        return userScheduleRepository.save(userSchedule);
    }

    public List<ScheduleDTO> getScheduleAsDTO(UserSchedule userSchedule) throws JsonProcessingException {
        return objectMapper.readValue(
            userSchedule.getSchedule(), 
            objectMapper.getTypeFactory().constructCollectionType(List.class, ScheduleDTO.class)
        );
    }

    public List<UserSchedule> getUserSchedules(Long userId, Long semesterId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (semesterId != null) {
            // Get semester info to filter by name
            Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found"));
            return userScheduleRepository.findByUserAndSemesterName(user, semester.getSemesterName());
        }
        
        return userScheduleRepository.findByUser(user);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        userScheduleRepository.deleteById(scheduleId);
    }

    @Transactional
    public UserSchedule updateSchedule(Long scheduleId, SaveScheduleRequest request) throws JsonProcessingException {
        UserSchedule existingSchedule = userScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        // Convert schedules list to JSON string
        String scheduleJson = objectMapper.writeValueAsString(request.getSchedules());
        
        // Update the schedule data and timestamp
        existingSchedule.setSchedule(scheduleJson);
        existingSchedule.setPrompt(request.getPrompt());
        existingSchedule.setParsedPrompt(request.getParsedPrompt());
        existingSchedule.setCreatedAt(java.time.LocalDateTime.now()); // Update timestamp
        
        return userScheduleRepository.save(existingSchedule);
    }
}
