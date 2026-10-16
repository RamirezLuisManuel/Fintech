package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.exception.DatoDuplicadoException;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.model.request.cuenta.CuentaIdentificadorDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Cuenta - CuentaServiceImpl")
class CuentaServiceImplTest {

    @Mock private CuentaRepository cuentaRepository;
    @Mock private ClienteRepository clienteRepository;
    @InjectMocks private CuentaServiceImpl cuentaService;

    private Cliente clienteActivo;
    private Cliente clienteInactivo;
    private Cuenta cuentaActiva;
    private Cuenta cuentaInactiva;

    @BeforeEach
    void setUp() {
        clienteActivo = new Cliente();
        clienteActivo.setId(1L);
        clienteActivo.setNombre("Pedro");
        clienteActivo.setActivo(true);

        clienteInactivo = new Cliente();
        clienteInactivo.setId(2L);
        clienteInactivo.setActivo(false);

        cuentaActiva = new Cuenta();
        cuentaActiva.setId(10L);
        cuentaActiva.setNumeroCuenta("1122334455");
        cuentaActiva.setCliente(clienteActivo);
        cuentaActiva.setSaldo(new BigDecimal("500.00"));
        cuentaActiva.setActivo(true);
        cuentaActiva.setUpdatedAt(LocalDateTime.now());

        cuentaInactiva = new Cuenta();
        cuentaInactiva.setId(20L);
        cuentaInactiva.setNumeroCuenta("9988776655");
        cuentaInactiva.setCliente(clienteActivo);
        cuentaInactiva.setActivo(false);
    }

    @Test
    @DisplayName("TC-CTASVC-01: Listar todas las cuentas exitoso retorna DTOs")
    void listarTodas_exitoso() {
        when(cuentaRepository.findAll()).thenReturn(List.of(cuentaActiva));
        List<CuentaResponseDTO> r = cuentaService.listarTodas();
        assertEquals(1, r.size());
        assertEquals("1122334455", r.get(0).getNumeroCuenta());
    }

    @Test
    @DisplayName("TC-CTASVC-02: Buscar cuenta ajena lanza OperacionNoPermitidaException")
    void buscarPorNumero_ajena_lanzaExcepcion() {
        CuentaIdentificadorDTO idDto = new CuentaIdentificadorDTO();
        idDto.setNumeroCuenta("1122334455");
        when(cuentaRepository.findByNumeroCuenta("1122334455")).thenReturn(Optional.of(cuentaActiva));

        assertThrows(OperacionNoPermitidaException.class,
                () -> cuentaService.buscarPorNumero(idDto, 999L));
    }

    @Test
    @DisplayName("TC-CTASVC-03: Crear cuenta genera 10 digitos y retorna CuentaResponseDTO")
    void crearCuenta_exitoso() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CuentaResponseDTO c = cuentaService.crearCuenta(1L);
        assertNotNull(c);
        assertEquals(10, c.getNumeroCuenta().length());
        assertEquals(new BigDecimal("0.00"), c.getSaldo());
        assertTrue(c.getActivo());
    }

    @Test
    @DisplayName("TC-CTASVC-04: Dar de baja cuenta ya inactiva lanza DatoDuplicadoException")
    void darDeBaja_yaInactiva_lanzaExcepcion() {
        CuentaIdentificadorDTO idDto = new CuentaIdentificadorDTO();
        idDto.setNumeroCuenta("9988776655");
        when(cuentaRepository.findByNumeroCuenta("9988776655")).thenReturn(Optional.of(cuentaInactiva));

        assertThrows(DatoDuplicadoException.class,
                () -> cuentaService.darDeBaja(idDto, 1L));
    }
}