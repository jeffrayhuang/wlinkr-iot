package com.wlinkr.iot.controller;

import com.wlinkr.iot.model.dto.UserDto;
import com.wlinkr.iot.model.entity.User;
import com.wlinkr.iot.model.enums.AuthProvider;
import com.wlinkr.iot.repository.UserRepository;
import com.wlinkr.iot.security.CurrentUser;
import com.wlinkr.iot.security.UserPrincipal;
import com.wlinkr.iot.security.JwtTokenProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication operations")
public class AuthController {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    @io.swagger.v3.oas.annotations.Operation(summary = "Register local user")
    @org.springframework.web.bind.annotation.PostMapping("/register")
    public org.springframework.http.ResponseEntity<?> register(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> req) {
        String name = req.getOrDefault("name", "").trim();
        String email = req.getOrDefault("email", "").trim().toLowerCase();
        String password = req.getOrDefault("password", "");
        if (name.isEmpty() || email.isEmpty() || password.length() < 6) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "Invalid input (min 6 char password)"));
        }
        if (userRepository.existsByEmail(email)) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "Email already registered"));
        }
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .provider(AuthProvider.LOCAL)
                .providerId(email)
                .build();
        userRepository.save(user);
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", "Registration successful"));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Login local user")
    @org.springframework.web.bind.annotation.PostMapping("/login")
    public org.springframework.http.ResponseEntity<?> login(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> req) {
        String email = req.getOrDefault("email", "").trim().toLowerCase();
        String password = req.getOrDefault("password", "");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of("message", "Invalid credentials"));
        }
        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtTokenProvider.generateToken(principal);
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("token", token));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<UserDto> getCurrentUser(@CurrentUser UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(new UserDto(
                user.getId(), user.getEmail(), user.getName(),
                user.getAvatarUrl(), user.getProvider(), user.getCreatedAt()
        ));
    }
}
