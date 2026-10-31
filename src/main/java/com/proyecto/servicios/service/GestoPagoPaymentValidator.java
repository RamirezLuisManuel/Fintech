package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaction;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.request.gestopago.PaymentChallengeRequest;
import com.proyecto.servicios.model.request.gestopago.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class GestoPagoPaymentValidator {

    private final GestoPagoGatewayService gatewayService;
    private final ObjectMapper objectMapper;

    public void validatePin(Usuario usuario, String rawPin, PasswordEncoder passwordEncoder) {
        if (usuario.getPin() == null || usuario.getPin().isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "El usuario no tiene configurado un PIN de seguridad");
        }
        if (!passwordEncoder.matches(rawPin, usuario.getPin())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "El PIN de autorizacion es incorrecto");
        }
    }

    public String resolveIdempotencyKey(PaymentRequest request, String header) {
        String key = StringUtils.isNotBlank(header)
                ? header.trim()
                : StringUtils.trimToNull(request.getIdempotencyKey());

        if (StringUtils.isBlank(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key es obligatorio (en header 'Idempotency-Key' o en el campo 'idempotencyKey' del JSON)");
        }
        return key;
    }

    public String resolveIdempotencyKey(PaymentChallengeRequest request, String header) {
        String key = StringUtils.isNotBlank(header)
                ? header.trim()
                : StringUtils.trimToNull(request.getIdempotencyKey());

        if (StringUtils.isBlank(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key es obligatorio (en header 'Idempotency-Key' o en el campo 'idempotencyKey' del JSON)");
        }
        return key;
    }

    public void validateResources(Usuario usuario, Cliente cliente, Cuenta cuenta) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new OperacionNoPermitidaException("El usuario se encuentra inactivo");
        }
        if (cliente == null || !Boolean.TRUE.equals(cliente.getActivo())) {
            throw new OperacionNoPermitidaException("El cliente se encuentra inactivo");
        }
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new OperacionNoPermitidaException("La cuenta se encuentra inactiva");
        }
        if (!cuenta.getCliente().getId().equals(cliente.getId())) {
            throw new OperacionNoPermitidaException("La cuenta no pertenece al usuario autenticado");
        }
    }

    public void validateActiveResources(GestoPagoTransaction tx) {
        if (!Boolean.TRUE.equals(tx.getUsuario().getActivo())) {
            throw new OperacionNoPermitidaException("El usuario se encuentra inactivo");
        }
        if (!Boolean.TRUE.equals(tx.getCliente().getActivo())) {
            throw new OperacionNoPermitidaException("El cliente se encuentra inactivo");
        }
        if (!Boolean.TRUE.equals(tx.getCuenta().getActivo())) {
            throw new OperacionNoPermitidaException("La cuenta se encuentra inactiva");
        }
        if (!tx.getCuenta().getCliente().getId().equals(tx.getCliente().getId())) {
            throw new OperacionNoPermitidaException("La cuenta no pertenece al cliente autenticado");
        }
    }

    public void validateAvailableBalance(Cuenta cuenta, BigDecimal amount) {
        BigDecimal held = cuenta.getSaldoRetenido() == null ? BigDecimal.ZERO : cuenta.getSaldoRetenido();
        if (cuenta.getSaldo().subtract(held).compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo disponible insuficiente para realizar el pago");
        }
    }

    public ValidatedPayment validateProductAndBuildInput(
            GestoPagoProduct product, PaymentRequest request) {
        return buildInput(product, request.getIdServicio(), request.getMonto(), request.getTelefono(), request.getReferencia());
    }

    public ValidatedPayment validateProductAndBuildInput(
            GestoPagoProduct product, PaymentChallengeRequest request) {
        return buildInput(product, request.getIdServicio(), request.getMonto(), request.getTelefono(), request.getReferencia());
    }

    private ValidatedPayment buildInput(
            GestoPagoProduct product, Integer requestIdServicio, BigDecimal monto, String rawTelefono, String rawReferencia) {
        if (product.getIdServicio() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El producto no tiene un idServicio valido en el catalogo local");
        }
        if (requestIdServicio != null && !requestIdServicio.equals(product.getIdServicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El idServicio enviado no corresponde con el producto seleccionado en el catalogo");
        }
        Integer tipoFront = product.getTipoFront();
        if (tipoFront == null || !List.of(1, 2, 4).contains(tipoFront)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El tipoFront del producto aun no esta soportado para pagos");
        }

        if (tipoFront == 1) {
            if (product.getPrecio() == null ||
                    BigDecimal.valueOf(product.getPrecio()).compareTo(monto) != 0) {
                String precioEsperado = product.getPrecio() != null ? String.format(java.util.Locale.US, "%.2f", product.getPrecio()) : "0.00";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El monto enviado ($" + monto + ") debe coincidir con el precio del producto seleccionado '" + product.getProducto() + "' ($" + precioEsperado + ")");
            }
            if (Integer.valueOf(10).equals(product.getIdCatTipoServicio()) ||
                    Integer.valueOf(11).equals(product.getIdCatTipoServicio())) {
                return new ValidatedPayment("#1111111111", null);
            }
            String phoneOrReference = StringUtils.isNotBlank(rawTelefono)
                    ? rawTelefono.trim()
                    : StringUtils.trimToNull(rawReferencia);
            if (StringUtils.isBlank(phoneOrReference) || phoneOrReference.length() < 3 || phoneOrReference.length() > 20 ||
                    !phoneOrReference.matches("[A-Za-z0-9#-]+")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El telefono o referencia del producto debe contener entre 3 y 20 caracteres validos");
            }
            if ("a".equalsIgnoreCase(product.getTipoReferencia()) && !phoneOrReference.matches("\\d{10}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El telefono debe contener exactamente 10 digitos");
            }
            return new ValidatedPayment(phoneOrReference, null);
        }

        String reference = StringUtils.trimToNull(rawReferencia);
        if (StringUtils.isBlank(reference) || reference.length() > 100 || !reference.matches("[A-Za-z0-9#-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La referencia debe ser alfanumerica y contener entre 1 y 100 caracteres");
        }
        return new ValidatedPayment("1111111111", reference);
    }

    @SneakyThrows
    public void verifyReference(GestoPagoProduct product, String reference) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idProducto", product.getIdProducto());
        payload.put("idServicio", product.getIdServicio());
        payload.put("referencia", reference);
        GestoPagoTxResponse response = gatewayService.verifyReference(objectMapper.writeValueAsString(payload));
        if (!"01".equals(response.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    response.getTexto() == null ? "La referencia no es valida" : response.getTexto());
        }
    }

    public void validateSameIdempotentRequest(
            GestoPagoTransaction tx, PaymentRequest request, Long authenticatedUserId) {
        validateSameIdempotent(tx, request.getNumeroCuenta(), request.getIdProducto(), request.getMonto(), request.getReferencia(), authenticatedUserId);
    }

    public void validateSameIdempotentRequest(
            GestoPagoTransaction tx, PaymentChallengeRequest request, Long authenticatedUserId) {
        validateSameIdempotent(tx, request.getNumeroCuenta(), request.getIdProducto(), request.getMonto(), request.getReferencia(), authenticatedUserId);
    }

    private void validateSameIdempotent(
            GestoPagoTransaction tx, String numeroCuenta, Integer idProducto, BigDecimal monto, String referencia, Long authenticatedUserId) {
        if (!tx.getUsuario().getId().equals(authenticatedUserId)) {
            throw new OperacionNoPermitidaException("La llave de idempotencia pertenece a otro usuario");
        }
        boolean same = tx.getCuenta().getNumeroCuenta().equals(numeroCuenta.trim())
                && tx.getIdProducto().equals(idProducto)
                && tx.getMonto().compareTo(monto) == 0
                && Objects.equals(tx.getReferencia(), StringUtils.trimToNull(referencia));
        if (!same) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La llave de idempotencia ya fue utilizada con datos diferentes");
        }
    }

    public record ValidatedPayment(String telefono, String referencia) {}
}