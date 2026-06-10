package com.example.UPImesh.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    String message,
    String status,
    LocalDateTime timestamp,
    Map<String, String> details
) {
    public ErrorResponse(String message, String status) {
        this(message, status, LocalDateTime.now(), null);
    }

    public ErrorResponse(String message, String status, Map<String, String> details) {
        this(message, status, LocalDateTime.now(), details);
    }
}

