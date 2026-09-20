package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class AuthRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    private String password;
}
