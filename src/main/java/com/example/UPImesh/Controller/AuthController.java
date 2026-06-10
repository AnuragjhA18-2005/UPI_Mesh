package com.example.UPImesh.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.example.UPImesh.security.JWTutil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired private JWTutil jwTutil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> credentials){
        String username = credentials.get("username");
        String password = credentials.get("password");

        if ("bridge-node-1".equals(username) && "secret123".equals(password)) {
            String token = jwTutil.generateToken(username);
            return ResponseEntity.ok(Map.of("token",token));
        }
        return ResponseEntity.status(401).body("Invalid Credentials");
    }
    
}

