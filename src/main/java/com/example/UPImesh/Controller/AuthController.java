package com.example.UPImesh.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.UPImesh.security.JWTutil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JWTutil jwtUtil;
    
    @Value("${app.auth.username}")
    private String configuredUsername;
    
    @Value("${app.auth.password}")
    private String configuredPassword;

    public AuthController(JWTutil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> credentials){
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (configuredUsername.equals(username) && configuredPassword.equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token",token));
        }
        return ResponseEntity.status(401).body("Invalid Credentials");
    }
    
}

