package com.example.user_service.dto;

import lombok.Data;

@Data
public class CreateAdminRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;
}
