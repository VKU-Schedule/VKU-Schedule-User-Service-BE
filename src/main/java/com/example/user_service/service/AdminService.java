package com.example.user_service.service;

import com.example.user_service.dto.AdminLoginRequest;
import com.example.user_service.dto.AdminResponse;
import com.example.user_service.dto.CreateAdminRequest;
import com.example.user_service.entity.Admin;
import com.example.user_service.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminResponse createAdmin(CreateAdminRequest request) {
        if (adminRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setFullName(request.getFullName());
        admin.setEmail(request.getEmail());
        admin.setIsActive(true);

        Admin saved = adminRepository.save(admin);
        return toResponse(saved);
    }

    public AdminResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!admin.getIsActive()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // Update last login
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        return toResponse(admin);
    }

    public List<AdminResponse> getAllAdmins() {
        return adminRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public AdminResponse getAdminById(Long id) {
        Admin admin = adminRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        return toResponse(admin);
    }

    public void deleteAdmin(Long id) {
        if (!adminRepository.existsById(id)) {
            throw new RuntimeException("Admin not found");
        }
        adminRepository.deleteById(id);
    }

    public AdminResponse toggleActive(Long id) {
        Admin admin = adminRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        admin.setIsActive(!admin.getIsActive());
        Admin saved = adminRepository.save(admin);
        return toResponse(saved);
    }

    private AdminResponse toResponse(Admin admin) {
        return new AdminResponse(
            admin.getId(),
            admin.getUsername(),
            admin.getFullName(),
            admin.getEmail(),
            admin.getIsActive(),
            admin.getCreatedAt(),
            admin.getLastLogin()
        );
    }
}
