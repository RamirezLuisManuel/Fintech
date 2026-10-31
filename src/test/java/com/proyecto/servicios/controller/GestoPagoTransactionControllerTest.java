package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.gestopago.PaymentConsultaRequest;
import com.proyecto.servicios.model.request.gestopago.PaymentRequest;
import com.proyecto.servicios.model.response.gestopago.PaymentTransactionResponse;
import com.proyecto.servicios.service.GestoPagoPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo GestoPago - GestoPagoTransactionController")
class GestoPagoTransactionControllerTest {

    @Mock
    private GestoPagoPaymentService paymentService;

    @InjectMocks
    private GestoPagoTransactionController controller;

    private CustomUserDetails principal;
    private UUID operationId;

    @BeforeEach
    void setUp() {
        operationId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setCorreo("test@utng.edu.mx");

        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setCliente(cliente);

        principal = new CustomUserDetails(usuario);
    }

    @Test
    @DisplayName("TC-TX-01: Pago unificado con PIN exitoso retorna 200 con GenericResponse codigo 0")
    void pagar_exitoso() {
        PaymentRequest request = PaymentRequest.builder()
                .numeroCuenta("1234567890")
                .monto(new BigDecimal("50.00"))
                .idProducto(1)
                .telefono("4181234567")
                .pin("1234")
                .build();

        PaymentTransactionResponse txResponse = PaymentTransactionResponse.builder()
                .operationId(operationId)
                .estado(GestoPagoTransactionStatus.SUCCEEDED)
                .monto(new BigDecimal("50.00"))
                .providerCode("01")
                .providerMessage("Operacion realizada con exito")
                .completedAt(LocalDateTime.now())
                .build();

        when(paymentService.procesarPago(any(PaymentRequest.class), eq("idemp-key-123"), eq(5L)))
                .thenReturn(txResponse);

        ResponseEntity<GenericResponse<PaymentTransactionResponse>> response =
                controller.pagar(request, "idemp-key-123", principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals("Pago procesado exitosamente", response.getBody().getMensaje());
        assertEquals(operationId, response.getBody().getData().getOperationId());
        assertEquals(GestoPagoTransactionStatus.SUCCEEDED, response.getBody().getData().getEstado());
        verify(paymentService).procesarPago(request, "idemp-key-123", 5L);
    }

    @Test
    @DisplayName("TC-TX-02: Detalle sin @PathVariable delega al servicio y retorna 200 con codigo 0")
    void detalle_sinPathVariable() {
        PaymentConsultaRequest request = PaymentConsultaRequest.builder()
                .operationId(operationId)
                .build();

        PaymentTransactionResponse txResponse = PaymentTransactionResponse.builder()
                .operationId(operationId)
                .estado(GestoPagoTransactionStatus.SUCCEEDED)
                .monto(new BigDecimal("50.00"))
                .build();

        when(paymentService.consultarPago(eq(request), eq(5L)))
                .thenReturn(txResponse);

        ResponseEntity<GenericResponse<PaymentTransactionResponse>> response =
                controller.detalle(request, principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals(operationId, response.getBody().getData().getOperationId());
        verify(paymentService).consultarPago(request, 5L);
    }

    @Test
    @DisplayName("TC-TX-03: Pago rechazado por proveedor retorna mensaje descriptivo del rechazo")
    void pagar_rechazado() {
        PaymentRequest request = PaymentRequest.builder()
                .numeroCuenta("1234567890")
                .monto(new BigDecimal("50.00"))
                .idProducto(1)
                .telefono("4181234567")
                .pin("1234")
                .build();

        PaymentTransactionResponse txResponse = PaymentTransactionResponse.builder()
                .operationId(operationId)
                .estado(GestoPagoTransactionStatus.FAILED)
                .monto(new BigDecimal("50.00"))
                .providerCode("44")
                .providerMessage("Servicio no asignado")
                .completedAt(LocalDateTime.now())
                .build();

        when(paymentService.procesarPago(any(PaymentRequest.class), any(), eq(5L)))
                .thenReturn(txResponse);

        ResponseEntity<GenericResponse<PaymentTransactionResponse>> response =
                controller.pagar(request, null, principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getMensaje().contains("Servicio no asignado"));
        assertEquals(GestoPagoTransactionStatus.FAILED, response.getBody().getData().getEstado());
    }
}