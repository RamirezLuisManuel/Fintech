package com.proyecto.servicios.model.request.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para consultar o gestionar un usuario sin usar @PathVariable.
 */
@Data
public class UsuarioIdentificadorDTO {
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long id;
}