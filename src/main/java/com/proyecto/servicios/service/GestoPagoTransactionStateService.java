package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaction;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GestoPagoTransactionStateService {

    private final GestoPagoTransactionRepository transactionRepository;
    private final CuentaRepository cuentaRepository;

    @Transactional
    public GestoPagoTransaction saveNew(GestoPagoTransaction transaction) {
        return transactionRepository.saveAndFlush(transaction);
    }

    @Transactional
    public GestoPagoTransaction createAndReserve(GestoPagoTransaction tx) {
        validateActiveResources(tx);

        int updated = cuentaRepository.reservarSaldo(tx.getCuenta().getId(), tx.getMonto());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo disponible insuficiente para realizar el pago");
        }

        tx.setEstado(GestoPagoTransactionStatus.PROCESSING);
        tx.setNextConfirmationAt(LocalDateTime.now().plusSeconds(62));
        return transactionRepository.saveAndFlush(tx);
    }

    @Transactional
    public GestoPagoTransaction reserveAndMarkProcessing(
            UUID operationId, Long userId, String horaLocal, String payloadJson) {
        GestoPagoTransaction tx = lockOwned(operationId, userId);
        if (tx.getEstado() != GestoPagoTransactionStatus.OTP_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La operacion no se encuentra pendiente de autenticacion");
        }
        validateActiveResources(tx);

        if (cuentaRepository.reservarSaldo(tx.getCuenta().getId(), tx.getMonto()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo disponible insuficiente para realizar el pago");
        }

        tx.setHoraLocal(horaLocal);
        tx.setPayloadJson(payloadJson);
        tx.setEstado(GestoPagoTransactionStatus.PROCESSING);
        tx.setNextConfirmationAt(LocalDateTime.now().plusSeconds(62));
        return transactionRepository.save(tx);
    }

    @Transactional
    public GestoPagoTransaction markSucceeded(UUID operationId, GestoPagoTxResponse response) {
        GestoPagoTransaction tx = lock(operationId);
        if (tx.getEstado() == GestoPagoTransactionStatus.SUCCEEDED) return tx;
        if (tx.getEstado() == GestoPagoTransactionStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La operacion ya fue marcada como fallida");
        }
        if (cuentaRepository.capturarSaldoRetenido(tx.getCuenta().getId(), tx.getMonto()) != 1) {
            throw new IllegalStateException("No fue posible capturar el saldo retenido de la operacion " + operationId);
        }
        applyProviderResponse(tx, response);
        tx.setEstado(GestoPagoTransactionStatus.SUCCEEDED);
        tx.setCompletedAt(LocalDateTime.now());
        tx.setNextConfirmationAt(null);
        return transactionRepository.save(tx);
    }

    @Transactional
    public GestoPagoTransaction markFailed(UUID operationId, GestoPagoTxResponse response) {
        GestoPagoTransaction tx = lock(operationId);
        if (tx.getEstado() == GestoPagoTransactionStatus.FAILED || tx.getEstado() == GestoPagoTransactionStatus.SUCCEEDED) {
            return tx;
        }
        if (cuentaRepository.liberarSaldoRetenido(tx.getCuenta().getId(), tx.getMonto()) != 1) {
            throw new IllegalStateException("No fue posible liberar el saldo retenido de la operacion " + operationId);
        }
        applyProviderResponse(tx, response);
        tx.setEstado(GestoPagoTransactionStatus.FAILED);
        tx.setCompletedAt(LocalDateTime.now());
        tx.setNextConfirmationAt(null);
        return transactionRepository.save(tx);
    }

    @Transactional
    public GestoPagoTransaction markUnknown(UUID operationId, GestoPagoTxResponse response, String fallbackMessage) {
        GestoPagoTransaction tx = lock(operationId);
        if (tx.getEstado() == GestoPagoTransactionStatus.SUCCEEDED || tx.getEstado() == GestoPagoTransactionStatus.FAILED) {
            return tx;
        }
        if (response != null) applyProviderResponse(tx, response);
        if (tx.getProviderMessage() == null) tx.setProviderMessage(fallbackMessage);
        tx.setEstado(GestoPagoTransactionStatus.UNKNOWN);
        tx.setNextConfirmationAt(LocalDateTime.now().plusSeconds(62));
        return transactionRepository.save(tx);
    }

    @Transactional
    public Optional<GestoPagoTransaction> claimForConfirmation(UUID operationId) {
        GestoPagoTransaction tx = lock(operationId);
        if (tx.getEstado() != GestoPagoTransactionStatus.UNKNOWN && tx.getEstado() != GestoPagoTransactionStatus.PROCESSING) {
            return Optional.empty();
        }
        if (tx.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            tx.setEstado(GestoPagoTransactionStatus.MANUAL_REVIEW);
            tx.setProviderMessage("No fue posible conciliar automaticamente la operacion dentro de 24 horas");
            tx.setNextConfirmationAt(null);
            transactionRepository.save(tx);
            return Optional.empty();
        }
        tx.setConfirmationAttempts(tx.getConfirmationAttempts() + 1);
        tx.setNextConfirmationAt(LocalDateTime.now().plusMinutes(5));
        return Optional.of(transactionRepository.save(tx));
    }

    @Transactional
    public void postponeConfirmation(UUID operationId, String message) {
        GestoPagoTransaction tx = lock(operationId);
        if (tx.getEstado() == GestoPagoTransactionStatus.UNKNOWN || tx.getEstado() == GestoPagoTransactionStatus.PROCESSING) {
            tx.setEstado(GestoPagoTransactionStatus.UNKNOWN);
            tx.setProviderMessage(message);
            tx.setNextConfirmationAt(LocalDateTime.now().plusMinutes(5));
            transactionRepository.save(tx);
        }
    }

    @Transactional
    public void markOtpDeliveryFailed(UUID operationId, String message) {
        GestoPagoTransaction tx = lock(operationId);
        if (tx.getEstado() == GestoPagoTransactionStatus.OTP_PENDING) {
            tx.setEstado(GestoPagoTransactionStatus.FAILED);
            tx.setProviderMessage(message);
            tx.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        }
    }

    @Transactional(readOnly = true)
    public GestoPagoTransaction getOwned(UUID operationId, Long userId) {
        GestoPagoTransaction tx = transactionRepository.findById(operationId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operacion de pago no encontrada"));
        if (!tx.getUsuario().getId().equals(userId)) {
            throw new OperacionNoPermitidaException("La operacion pertenece a otro usuario");
        }
        return tx;
    }

    private GestoPagoTransaction lockOwned(UUID operationId, Long userId) {
        GestoPagoTransaction tx = lock(operationId);
        if (!tx.getUsuario().getId().equals(userId)) {
            throw new OperacionNoPermitidaException("La operacion pertenece a otro usuario");
        }
        return tx;
    }

    private GestoPagoTransaction lock(UUID operationId) {
        return transactionRepository.findByIdForUpdate(operationId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operacion de pago no encontrada"));
    }

    private void validateActiveResources(GestoPagoTransaction tx) {
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

    private void applyProviderResponse(GestoPagoTransaction tx, GestoPagoTxResponse response) {
        if (response == null) return;
        tx.setProviderCode(response.getCodigo());
        tx.setProviderMessage(response.getTexto());
        tx.setProviderTransactionId(response.getIdTx());
        tx.setProviderAuthorization(response.getNumeroAutorizacion());
        tx.setPin(response.getPin());
        tx.setLegend(response.getLegend());
    }
}