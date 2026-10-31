package com.proyecto.servicios.model.request.gestopago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequest {

    @NotNull(message = "El operationId es obligatorio")
    private UUID operationId;

    @NotBlank(message = "El codigo de autenticacion es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El codigo de autenticacion debe contener 6 digitos")
    private String codigo;
}
