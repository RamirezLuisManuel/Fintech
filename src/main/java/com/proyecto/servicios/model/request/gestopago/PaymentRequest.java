package com.proyecto.servicios.model.request.gestopago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Size(max = 20, message = "El numero de cuenta no puede exceder 20 caracteres")
    private String numeroCuenta;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 22, fraction = 2, message = "El monto solo puede tener dos decimales")
    private BigDecimal monto;

    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser positivo")
    private Integer idProducto;

    @Positive(message = "El idServicio debe ser positivo")
    private Integer idServicio;

    @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
    private String telefono;

    @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
    private String referencia;

    @Size(max = 64, message = "Idempotency-Key no puede exceder 64 caracteres")
    private String idempotencyKey;

    @NotBlank(message = "El PIN de seguridad es obligatorio")
    @Pattern(regexp = "\\d{4,6}", message = "El PIN debe contener entre 4 y 6 digitos numericos")
    private String pin;
}