package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.gestopago.PaymentConsultaRequest;
import com.proyecto.servicios.model.request.gestopago.PaymentRequest;
import com.proyecto.servicios.model.response.gestopago.PaymentTransactionResponse;
import com.proyecto.servicios.service.GestoPagoPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gestopago/transactions")
@RequiredArgsConstructor
public class GestoPagoTransactionController {

    private final GestoPagoPaymentService paymentService;

    @PostMapping("/pagar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<PaymentTransactionResponse>> pagar(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PaymentTransactionResponse response = paymentService.procesarPago(
                request, idempotencyKeyHeader, principal.getUsuarioId());
        String mensaje = switch (response.getEstado()) {
            case SUCCEEDED -> "Pago procesado exitosamente";
            case FAILED -> "Pago rechazado por el proveedor: " + (response.getProviderMessage() != null ? response.getProviderMessage() : "Transacción rechazada");
            case UNKNOWN, PROCESSING -> "Pago en proceso de confirmación con el proveedor";
            default -> "Operación procesada";
        };
        return ResponseEntity.ok(GenericResponse.exito(mensaje, response));
    }

    @PostMapping("/detalle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<PaymentTransactionResponse>> detalle(
            @Valid @RequestBody PaymentConsultaRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(GenericResponse.exito(
                paymentService.consultarPago(request, principal.getUsuarioId())));
    }
}