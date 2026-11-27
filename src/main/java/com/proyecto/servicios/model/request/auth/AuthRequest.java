package com.proyecto.servicios.model.request.auth;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class AuthRequest {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo invalido")
    private String correo;
    
    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    private String codigo2fa;
    private Boolean forceClosePrevious;
    private String platform;
}
