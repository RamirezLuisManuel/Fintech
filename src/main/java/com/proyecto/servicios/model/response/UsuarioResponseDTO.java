package com.proyecto.servicios.model.response;

import com.proyecto.servicios.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Long id;
    private String correo;
    private String rol;
    private Boolean activo;
    private Long clienteId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UsuarioResponseDTO fromEntity(Usuario u) {
        if (u == null) return null;
        return UsuarioResponseDTO.builder()
                .id(u.getId())
                .correo(u.getCliente() != null ? u.getCliente().getCorreo() : null)
                .rol(u.getRol())
                .activo(u.getActivo())
                .clienteId(u.getCliente() != null ? u.getCliente().getId() : null)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}