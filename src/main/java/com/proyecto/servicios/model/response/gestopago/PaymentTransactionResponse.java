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
    private GestoPagoTransactionStatus estado;
    private BigDecimal monto;
    private Integer idProducto;
    private Integer idServicio;
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
                .estado(tx.getEstado())
                .monto(tx.getMonto())
                .idProducto(tx.getIdProducto())
                .idServicio(tx.getIdServicio())
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
