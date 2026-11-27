package com.proyecto.servicios.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroClienteResponseDTO {
    private String numeroCuenta;
    private String pin;
    private String aviso;
    private String totpSecret;
    private String totpUri;

    public RegistroClienteResponseDTO(String numeroCuenta, String pin, String aviso) {
        this.numeroCuenta = numeroCuenta;
        this.pin = pin;
        this.aviso = aviso;
    }
}

