package com.proyecto.servicios.model.request.gestopago;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConsultaRequest {
    @NotNull(message = "El operationId es obligatorio")
    private UUID operationId;
}
