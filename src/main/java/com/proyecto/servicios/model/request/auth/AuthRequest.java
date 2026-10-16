package com.proyecto.servicios.model.request.auth;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class AuthRequest {
    @NotBlank
    @Email
    private String correo;
    
    @NotBlank
    private String password;
}
