package com.example.user_service.config;

import com.example.user_service.entity.Admin;
import com.example.user_service.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        initializeDefaultAdmin();
    }

    private void initializeDefaultAdmin() {
        // Check if any admin exists
        if (adminRepository.count() == 0) {
            log.info("No admin found. Creating default admin account...");
            
            Admin defaultAdmin = new Admin();
            defaultAdmin.setUsername("admin");
            defaultAdmin.setPassword(passwordEncoder.encode("admin123"));
            defaultAdmin.setFullName("System Administrator");
            defaultAdmin.setEmail("admin@example.com");
            defaultAdmin.setIsActive(true);
            
            adminRepository.save(defaultAdmin);
            
            log.info("Default admin account created successfully!");
            log.info("Username: admin");
            log.info("Password: admin123");
            log.info("Please change the password after first login!");
        } else {
            log.info("Admin accounts already exist. Skipping initialization.");
        }
    }
}
