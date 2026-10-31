package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaction;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.request.gestopago.PaymentChallengeRequest;
import com.proyecto.servicios.model.request.gestopago.PaymentConfirmRequest;
import com.proyecto.servicios.model.request.gestopago.PaymentConsultaRequest;
import com.proyecto.servicios.model.request.gestopago.PaymentRequest;
import com.proyecto.servicios.model.response.gestopago.PaymentChallengeResponse;
import com.proyecto.servicios.model.response.gestopago.PaymentTransactionResponse;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class GestoPagoPaymentService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter GESTOPAGO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<GestoPagoTransactionStatus> RECONCILABLE = List.of(
            GestoPagoTransactionStatus.PROCESSING, GestoPagoTransactionStatus.UNKNOWN);

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final GestoPagoProductRepository productRepository;
    private final GestoPagoTransactionRepository transactionRepository;
    private final GestoPagoTransactionStateService stateService;
    private final PaymentOtpService otpService;
    private final PaymentEmailService emailService;
    private final GestoPagoGatewayService gatewayService;
    private final ObjectMapper objectMapper;
    private final GestoPagoPaymentValidator paymentValidator;
    private final PasswordEncoder passwordEncoder;

    @Value("${gestopago.commercial-unit:083}")
    private String commercialUnit;

    @Value("${gestopago.time-zone:America/Mexico_City}")
    private String timeZone;

    // Constructor de compatibilidad para pruebas unitarias existentes
    public GestoPagoPaymentService(
            UsuarioRepository usuarioRepository, CuentaRepository cuentaRepository,
            GestoPagoProductRepository productRepository, GestoPagoTransactionRepository transactionRepository,
            GestoPagoTransactionStateService stateService, PaymentOtpService otpService,
            PaymentEmailService emailService, GestoPagoGatewayService gatewayService, ObjectMapper objectMapper) {
        this(usuarioRepository, cuentaRepository, productRepository, transactionRepository,
                stateService, otpService, emailService, gatewayService, objectMapper,
                new GestoPagoPaymentValidator(gatewayService, objectMapper),
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
    }

    /**
     * Endpoint unificado de pago con PIN de autorizacion (Un solo paso)
     */
    public PaymentTransactionResponse procesarPago(
            PaymentRequest request, String idempotencyKeyHeader, Long authenticatedUserId) {
        String idempotencyKey = paymentValidator.resolveIdempotencyKey(request, idempotencyKeyHeader);

        // 1. Validar idempotencia si ya fue procesada
        GestoPagoTransaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            paymentValidator.validateSameIdempotentRequest(existing, request, authenticatedUserId);
            return PaymentTransactionResponse.fromEntity(existing);
        }

        // 2. Cargar entidades
        Usuario usuario = usuarioRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
        Cliente cliente = usuario.getCliente();
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));
        GestoPagoProduct product = productRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto de GestoPago no encontrado"));

        // 3. Validar PIN de seguridad hasheado en BD
        paymentValidator.validatePin(usuario, request.getPin(), passwordEncoder);

        // 4. Validar recursos, saldo y reglas de producto
        paymentValidator.validateResources(usuario, cliente, cuenta);
        paymentValidator.validateAvailableBalance(cuenta, request.getMonto());
        GestoPagoPaymentValidator.ValidatedPayment validated =
                paymentValidator.validateProductAndBuildInput(product, request);

        if (product.getTipoFront() == 4) {
            paymentValidator.verifyReference(product, validated.referencia());
        }

        // 5. Crear transaccion y reservar saldo atomicamente
        String upc = generateUniqueUpc();
        String horaLocal = LocalDateTime.now(ZoneId.of(timeZone)).format(GESTOPAGO_TIME);

        GestoPagoTransaction tx = new GestoPagoTransaction();
        tx.setId(UUID.randomUUID());
        tx.setIdempotencyKey(idempotencyKey);
        tx.setUpc(upc);
        tx.setUsuario(usuario);
        tx.setCliente(cliente);
        tx.setCuenta(cuenta);
        tx.setIdProducto(product.getIdProducto());
        tx.setIdServicio(product.getIdServicio());
        tx.setTipoFront(product.getTipoFront());
        tx.setIdCatTipoServicio(product.getIdCatTipoServicio());
        tx.setTelefono(validated.telefono());
        tx.setReferencia(validated.referencia());
        tx.setMonto(request.getMonto());
        tx.setUnidad(commercialUnit);
        tx.setHoraLocal(horaLocal);

        String payloadJson = buildSendPayloadJson(tx, horaLocal);
        tx.setPayloadJson(payloadJson);

        try {
            tx = stateService.createAndReserve(tx);
        } catch (DataIntegrityViolationException ex) {
            GestoPagoTransaction concurrent = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
            paymentValidator.validateSameIdempotentRequest(concurrent, request, authenticatedUserId);
            return PaymentTransactionResponse.fromEntity(concurrent);
        }

        // 6. Enviar a GestoPago sendTx.do
        try {
            GestoPagoTxResponse response = gatewayService.sendTx(payloadJson);
            GestoPagoTransaction result = response.isSuccessfulSendTx()
                    ? stateService.markSucceeded(tx.getId(), response)
                    : ("06".equals(response.getCodigo()) || response.getCodigo() == null)
                    ? stateService.markUnknown(tx.getId(), response, "GestoPago no confirmo de forma concluyente")
                    : stateService.markFailed(tx.getId(), response);
            return PaymentTransactionResponse.fromEntity(result);
        } catch (RuntimeException ex) {
            log.warn("Resultado incierto en sendTx para operationId={}: {}", tx.getId(), ex.getMessage());
            GestoPagoTransaction unknown = stateService.markUnknown(
                    tx.getId(), null, "Sin respuesta concluyente de GestoPago; pendiente de confirmacion");
            return PaymentTransactionResponse.fromEntity(unknown);
        }
    }

    public PaymentTransactionResponse consultarPago(PaymentConsultaRequest request, Long authenticatedUserId) {
        return get(request.getOperationId(), authenticatedUserId);
    }

    public PaymentTransactionResponse get(UUID operationId, Long authenticatedUserId) {
        return PaymentTransactionResponse.fromEntity(stateService.getOwned(operationId, authenticatedUserId));
    }

    @Scheduled(fixedDelayString = "${gestopago.confirmation.fixed-delay-ms:30000}")
    public void reconcileUnknownTransactions() {
        transactionRepository.findIdsReadyForConfirmation(RECONCILABLE, LocalDateTime.now())
                .stream().limit(100).forEach(this::reconcileOne);
    }

    private void reconcileOne(UUID operationId) {
        try {
            stateService.claimForConfirmation(operationId).ifPresent(tx -> {
                try {
                    GestoPagoTxResponse response = gatewayService.confirmTx(tx.getPayloadJson());
                    if (response.isConfirmedAsApplied()) {
                        stateService.markSucceeded(operationId, response);
                    } else if (response.isConfirmedAsNotApplied()) {
                        stateService.markFailed(operationId, response);
                    } else {
                        stateService.postponeConfirmation(operationId,
                                "confirmTx aun no devuelve resultado concluyente: " + response.getTexto());
                    }
                } catch (Exception ex) {
                    stateService.postponeConfirmation(operationId,
                            "No fue posible consultar confirmTx: " + ex.getClass().getSimpleName());
                }
            });
        } catch (Exception ex) {
            log.error("Error conciliando operationId={}", operationId, ex);
        }
    }

    @SneakyThrows
    private String buildSendPayloadJson(GestoPagoTransaction tx, String horaLocal) {
        boolean servicePayment = tx.getTipoFront() == 2 || tx.getTipoFront() == 4;
        return objectMapper.writeValueAsString(GestoPagoSendTxPayload.builder()
                .idProducto(tx.getIdProducto())
                .idServicio(tx.getIdServicio())
                .telefono(tx.getTelefono())
                .horaLocal(horaLocal)
                .referencia(servicePayment ? tx.getReferencia() : null)
                .montoPago(servicePayment ? tx.getMonto() : null)
                .upc(tx.getUpc())
                .unidad(tx.getUnidad())
                .build());
    }

    private String generateUniqueUpc() {
        String upc;
        do {
            String time = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
            String random = RandomStringUtils.random(5, 0, 0, true, true, null, SECURE_RANDOM).toUpperCase();
            upc = (time + random);
            if (upc.length() > 15) upc = upc.substring(upc.length() - 15);
        } while (transactionRepository.existsByUpc(upc));
        return upc;
    }
}