package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class OnboardingException extends RuntimeException {
    
    private final HttpStatus status;

    public OnboardingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
