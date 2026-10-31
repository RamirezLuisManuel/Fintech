package com.proyecto.servicios.model.response;

import com.proyecto.servicios.entity.Cuenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaResponseDTO {
    private Long id;
    private String numeroCuenta;
    private BigDecimal saldo;
    private BigDecimal saldoRetenido;
    private BigDecimal saldoDisponible;
    private Boolean activo;
    private Long clienteId;
    private String clienteNombre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CuentaResponseDTO fromEntity(Cuenta c) {
        if (c == null) return null;
        return CuentaResponseDTO.builder()
                .id(c.getId())
                .numeroCuenta(c.getNumeroCuenta())
                .saldo(c.getSaldo())
                .saldoRetenido(c.getSaldoRetenido())
                .saldoDisponible(c.getSaldo().subtract(c.getSaldoRetenido()))
                .activo(c.getActivo())
                .clienteId(c.getCliente() != null ? c.getCliente().getId() : null)
                .clienteNombre(c.getCliente() != null ? c.getCliente().getNombre() + " " + c.getCliente().getApellidoPaterno() : null)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
