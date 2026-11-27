package com.proyecto.servicios.model.response.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaction;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentTransactionResponse {
    private UUID operationId;
    private String upc;
    private GestoPagoTransactionStatus estado;
    private BigDecimal monto;
    private Integer idProducto;
    private Integer idServicio;
    private Integer tipoFront;
    private String productoNombre;
    private String servicioNombre;
    private String telefono;
    private String referencia;
    private String numeroCuenta;
    private String providerCode;
    private String providerMessage;
    private String providerTransactionId;
    private String providerAuthorization;
    private String pin;
    private String legend;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static PaymentTransactionResponse fromEntity(GestoPagoTransaction tx) {
        return PaymentTransactionResponse.builder()
                .operationId(tx.getId())
                .upc(tx.getUpc())
                .estado(tx.getEstado())
                .monto(tx.getMonto())
                .idProducto(tx.getIdProducto())
                .idServicio(tx.getIdServicio())
                .tipoFront(tx.getTipoFront())
                .telefono(tx.getTelefono())
                .referencia(tx.getReferencia())
                .numeroCuenta(tx.getCuenta() != null ? tx.getCuenta().getNumeroCuenta() : null)
                .providerCode(tx.getProviderCode())
                .providerMessage(tx.getProviderMessage())
                .providerTransactionId(tx.getProviderTransactionId())
                .providerAuthorization(tx.getProviderAuthorization())
                .pin(tx.getPin())
                .legend(tx.getLegend())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}
