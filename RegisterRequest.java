package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.dto.AuthResponse;
import com.startup1.startup1_backend.dto.LoginRequest;
import com.startup1.startup1_backend.dto.RegisterRequest;
import com.startup1.startup1_backend.dto.ResetPasswordRequest;
import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.UserRepository;
import com.startup1.startup1_backend.security.JwtService;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (isBlank(request.getEmail()) || isBlank(request.getPassword()) || isBlank(request.getRole())) {
            return ResponseEntity.badRequest().body("Email, password, and role are required.");
        }

        if (userRepository.existsByEmail(request.getEmail().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("An account already exists for that email.");
        }

        String role = normalizeRole(request.getRole());
        String resolvedName = resolveName(request);
        if (isBlank(resolvedName)) {
            return ResponseEntity.badRequest().body("A name or business name is required.");
        }

        User user = new User();
        user.setRole(role);
        user.setName(resolvedName.trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setAddress(trimToNull(request.getAddress()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (isBlank(request.getEmailOrPhone()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().body("Email/phone and password are required.");
        }

        Optional<User> userOpt = findByEmailOrPhone(request.getEmailOrPhone());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }

        return ResponseEntity.ok(buildResponse(userOpt.get()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (isBlank(request.getEmailOrPhone()) || isBlank(request.getNewPassword())) {
            return ResponseEntity.badRequest().body("Email/phone and new password are required.");
        }

        Optional<User> userOpt = findByEmailOrPhone(request.getEmailOrPhone());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No account matches that email or phone.");
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Password reset successful.");
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getId(), user.getRole(), user.getName(), user.getEmail());
    }

    private Optional<User> findByEmailOrPhone(String emailOrPhone) {
        String value = emailOrPhone.trim();
        if (value.contains("@")) {
            return userRepository.findByEmail(value.toLowerCase(Locale.ROOT));
        }
        return userRepository.findByPhone(value);
    }

    private String resolveName(RegisterRequest request) {
        String role = normalizeRole(request.getRole());
        if ("HOSPITAL".equals(role) || "PHARMACY".equals(role)) {
            return isBlank(request.getBusinessName()) ? request.getName() : request.getBusinessName();
        }
        return request.getName();
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
