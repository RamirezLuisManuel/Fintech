package com.proyecto.servicios.model.response.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentChallengeResponse {
    private UUID operationId;
    private GestoPagoTransactionStatus estado;
    private String correoDestino;
    private LocalDateTime codigoExpiraEn;
}
