package com.proyecto.servicios.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String numeroCuenta;
    private Long clienteId;
    private String nombreCompleto;
    private String correo;
    private Boolean requires2fa;
    private Boolean activeSessionDetected;
    private String activeSessionInfo;
    private String mensaje2fa;
    private String totpSecret;
    private String totpUri;

    public AuthResponse(String accessToken, String refreshToken, String numeroCuenta) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.numeroCuenta = numeroCuenta;
    }
}
