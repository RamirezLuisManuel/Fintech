package com.proyecto.servicios.model.request.cuenta;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para consultar o gestionar una cuenta sin usar @PathVariable.
 */
@Data
public class CuentaIdentificadorDTO {
    @NotBlank(message = "El numero de cuenta es obligatorio")
    private String numeroCuenta;
}