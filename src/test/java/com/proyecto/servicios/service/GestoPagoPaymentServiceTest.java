package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaction;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.request.gestopago.PaymentRequest;
import com.proyecto.servicios.model.response.gestopago.PaymentTransactionResponse;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoPaymentServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CuentaRepository cuentaRepository;
    @Mock private GestoPagoProductRepository productRepository;
    @Mock private GestoPagoTransactionRepository transactionRepository;
    @Mock private GestoPagoTransactionStateService stateService;
    @Mock private PaymentOtpService otpService;
    @Mock private PaymentEmailService emailService;
    @Mock private GestoPagoGatewayService gatewayService;

    private GestoPagoPaymentService service;
    private Usuario usuario;
    private Cliente cliente;
    private Cuenta cuenta;
    private GestoPagoProduct product;
    private PaymentRequest request;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        service = new GestoPagoPaymentService(usuarioRepository, cuentaRepository, productRepository,
                transactionRepository, stateService, otpService, emailService, gatewayService, new ObjectMapper());
        ReflectionTestUtils.setField(service, "commercialUnit", "083");
        ReflectionTestUtils.setField(service, "timeZone", "America/Mexico_City");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setActivo(true);
        cliente.setCorreo("alumno@utng.edu.mx");

        usuario = new Usuario();
        usuario.setId(2L);
        usuario.setActivo(true);
        usuario.setCliente(cliente);
        usuario.setPin(passwordEncoder.encode("1234"));

        cuenta = new Cuenta();
        cuenta.setId(3L);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setCliente(cliente);
        cuenta.setActivo(true);
        cuenta.setSaldo(new BigDecimal("1000.00"));
        cuenta.setSaldoRetenido(BigDecimal.ZERO);

        product = new GestoPagoProduct();
        product.setIdProducto(582);
        product.setIdServicio(133);
        product.setTipoFront(1);
        product.setIdCatTipoServicio(2);
        product.setTipoReferencia("a");
        product.setPrecio(100.00);

        request = new PaymentRequest();
        request.setIdempotencyKey("idem-1");
        request.setNumeroCuenta("1234567890");
        request.setMonto(new BigDecimal("100.00"));
        request.setIdProducto(582);
        request.setTelefono("5555555551");
        request.setPin("1234");
    }

    @Test
    void procesarPago_exitoso() {
        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(transactionRepository.existsByUpc(any())).thenReturn(false);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productRepository.findById(582)).thenReturn(Optional.of(product));
        when(stateService.createAndReserve(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GestoPagoTxResponse txResp = GestoPagoTxResponse.builder()
                .codigo("01")
                .texto("Transaccion Exitosa")
                .idTx("GP-8888")
                .numeroAutorizacion("AUTH-999")
                .build();
        when(gatewayService.sendTx(any())).thenReturn(txResp);

        GestoPagoTransaction completedTx = new GestoPagoTransaction();
        completedTx.setEstado(GestoPagoTransactionStatus.SUCCEEDED);
        completedTx.setMonto(new BigDecimal("100.00"));
        completedTx.setProviderTransactionId("GP-8888");
        completedTx.setProviderAuthorization("AUTH-999");
        completedTx.setProviderCode("01");
        completedTx.setProviderMessage("Transaccion Exitosa");
        when(stateService.markSucceeded(any(), eq(txResp))).thenReturn(completedTx);

        PaymentTransactionResponse response = service.procesarPago(request, null, 2L);

        assertNotNull(response);
        assertEquals(GestoPagoTransactionStatus.SUCCEEDED, response.getEstado());
        assertEquals("GP-8888", response.getProviderTransactionId());

        ArgumentCaptor<GestoPagoTransaction> captor = ArgumentCaptor.forClass(GestoPagoTransaction.class);
        verify(stateService).createAndReserve(captor.capture());
        GestoPagoTransaction saved = captor.getValue();
        assertEquals(133, saved.getIdServicio());
        assertEquals("083", saved.getUnidad());
        assertEquals("5555555551", saved.getTelefono());
        assertEquals(new BigDecimal("100.00"), saved.getMonto());
        verify(gatewayService).sendTx(any());
        verify(stateService).markSucceeded(any(), eq(txResp));
    }

    @Test
    void procesarPago_rejectsPinIncorrecto() {
        request.setPin("9999");
        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productRepository.findById(582)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.procesarPago(request, null, 2L));
        assertTrue(ex.getReason().contains("PIN"));
        verify(stateService, never()).createAndReserve(any());
    }

    @Test
    void procesarPago_rejectsInactiveClient() {
        cliente.setActivo(false);
        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productRepository.findById(582)).thenReturn(Optional.of(product));

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.procesarPago(request, null, 2L));
        verify(stateService, never()).createAndReserve(any());
    }

    @Test
    void procesarPago_rejectsManipulatedFixedPrice() {
        request.setMonto(new BigDecimal("50.00"));
        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productRepository.findById(582)).thenReturn(Optional.of(product));

        assertThrows(ResponseStatusException.class,
                () -> service.procesarPago(request, null, 2L));
        verify(stateService, never()).createAndReserve(any());
    }
}
