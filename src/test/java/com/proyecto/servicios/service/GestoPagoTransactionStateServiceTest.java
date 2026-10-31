package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaction;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTransactionStateServiceTest {

    @Mock private GestoPagoTransactionRepository transactionRepository;
    @Mock private CuentaRepository cuentaRepository;
    @InjectMocks private GestoPagoTransactionStateService service;

    private UUID operationId;
    private GestoPagoTransaction transaction;

    @BeforeEach
    void setUp() {
        operationId = UUID.randomUUID();
        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setActivo(true);

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setCliente(cliente);
        usuario.setActivo(true);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(30L);
        cuenta.setCliente(cliente);
        cuenta.setActivo(true);
        cuenta.setSaldo(new BigDecimal("1000.00"));

        transaction = new GestoPagoTransaction();
        transaction.setId(operationId);
        transaction.setUsuario(usuario);
        transaction.setCliente(cliente);
        transaction.setCuenta(cuenta);
        transaction.setMonto(new BigDecimal("150.00"));
        transaction.setEstado(GestoPagoTransactionStatus.OTP_PENDING);
    }

    @Test
    void reserveAndMarkProcessing_reservesAtomically() {
        when(transactionRepository.findByIdForUpdate(operationId)).thenReturn(Optional.of(transaction));
        when(cuentaRepository.reservarSaldo(30L, new BigDecimal("150.00"))).thenReturn(1);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        GestoPagoTransaction result = service.reserveAndMarkProcessing(
                operationId, 20L, "20261019235900", "{\"payload\":true}");

        assertEquals(GestoPagoTransactionStatus.PROCESSING, result.getEstado());
        assertEquals("20261019235900", result.getHoraLocal());
        assertNotNull(result.getNextConfirmationAt());
        verify(cuentaRepository).reservarSaldo(30L, new BigDecimal("150.00"));
    }

    @Test
    void reserveAndMarkProcessing_rejectsInsufficientAvailableBalance() {
        when(transactionRepository.findByIdForUpdate(operationId)).thenReturn(Optional.of(transaction));
        when(cuentaRepository.reservarSaldo(30L, new BigDecimal("150.00"))).thenReturn(0);

        assertThrows(ResponseStatusException.class, () -> service.reserveAndMarkProcessing(
                operationId, 20L, "20261019235900", "{}"));
        assertEquals(GestoPagoTransactionStatus.OTP_PENDING, transaction.getEstado());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void markSucceeded_capturesHeldBalanceAndStoresProviderData() {
        transaction.setEstado(GestoPagoTransactionStatus.PROCESSING);
        when(transactionRepository.findByIdForUpdate(operationId)).thenReturn(Optional.of(transaction));
        when(cuentaRepository.capturarSaldoRetenido(30L, new BigDecimal("150.00"))).thenReturn(1);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        GestoPagoTxResponse response = GestoPagoTxResponse.builder()
                .codigo("01").texto("Operacion realizada con exito")
                .idTx("123").numeroAutorizacion("AUTH-1").build();

        GestoPagoTransaction result = service.markSucceeded(operationId, response);

        assertEquals(GestoPagoTransactionStatus.SUCCEEDED, result.getEstado());
        assertEquals("123", result.getProviderTransactionId());
        assertEquals("AUTH-1", result.getProviderAuthorization());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void markFailed_releasesHeldBalance() {
        transaction.setEstado(GestoPagoTransactionStatus.PROCESSING);
        when(transactionRepository.findByIdForUpdate(operationId)).thenReturn(Optional.of(transaction));
        when(cuentaRepository.liberarSaldoRetenido(30L, new BigDecimal("150.00"))).thenReturn(1);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        GestoPagoTransaction result = service.markFailed(operationId,
                GestoPagoTxResponse.builder().codigo("02").texto("Saldo insuficiente").build());

        assertEquals(GestoPagoTransactionStatus.FAILED, result.getEstado());
        verify(cuentaRepository).liberarSaldoRetenido(30L, new BigDecimal("150.00"));
    }
}
