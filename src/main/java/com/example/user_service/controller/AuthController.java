package com.example.user_service.controller;

import com.example.user_service.entity.User;
import com.example.user_service.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        // Validate VKU email
        if (email == null || !email.endsWith("@vku.udn.vn")) {
            return ResponseEntity.status(403).build();
        }

        User user = userService.findOrCreateUser(email, name, picture);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/login-success")
    public ResponseEntity<Map<String, String>> loginSuccess() {
        return ResponseEntity.ok(Map.of("message", "Login successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            String credential = payload.get("credential");
            
            if (credential == null || credential.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Credential is required"));
            }

            // Verify Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), 
                GsonFactory.getDefaultInstance()
            )
            .setAudience(Collections.singletonList(googleClientId))
            .build();

            GoogleIdToken idToken = verifier.verify(credential);
            
            if (idToken == null) {
                return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid credential"));
            }

            GoogleIdToken.Payload googlePayload = idToken.getPayload();
            String email = googlePayload.getEmail();
            String name = (String) googlePayload.get("name");
            String picture = (String) googlePayload.get("picture");

            // Validate VKU email
            if (email == null || !email.endsWith("@vku.udn.vn")) {
                return ResponseEntity.status(403)
                    .body(Map.of("message", "Chỉ chấp nhận email @vku.udn.vn"));
            }

            // Find or create user
            User user = userService.findOrCreateUser(email, name, picture);
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }
}
