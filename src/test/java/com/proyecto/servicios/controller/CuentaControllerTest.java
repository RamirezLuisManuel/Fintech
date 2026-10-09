package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Cuenta - CuentaController")
class CuentaControllerTest {

    @Mock private CuentaRepository cuentaRepository;
    @Mock private ClienteRepository clienteRepository;
    @InjectMocks private CuentaController cuentaController;

    private Cuenta cuentaActiva;
    private Cuenta cuentaInactiva;
    private Cliente clienteActivo;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        clienteActivo = new Cliente();
        clienteActivo.setId(1L);
        clienteActivo.setNombre("Juan");
        clienteActivo.setActivo(true);

        cuentaActiva = new Cuenta();
        cuentaActiva.setId(1L);
        cuentaActiva.setNumeroCuenta("1234567890");
        cuentaActiva.setCliente(clienteActivo);
        cuentaActiva.setActivo(true);
        cuentaActiva.setSaldo(new BigDecimal("1000.00"));
        cuentaActiva.setCreatedAt(LocalDateTime.now());
        cuentaActiva.setUpdatedAt(LocalDateTime.now());

        cuentaInactiva = new Cuenta();
        cuentaInactiva.setId(2L);
        cuentaInactiva.setNumeroCuenta("0987654321");
        cuentaInactiva.setCliente(clienteActivo);
        cuentaInactiva.setActivo(false);
        cuentaInactiva.setUpdatedAt(LocalDateTime.now());

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCliente(clienteActivo);
        usuario.setActivo(true);
        principal = new CustomUserDetails(usuario);
    }

    @Test @DisplayName("TC-CTA-01: Listar todas las cuentas retorna lista completa")
    void getAllCuentas_retornaLista() {
        when(cuentaRepository.findAll()).thenReturn(Arrays.asList(cuentaActiva, cuentaInactiva));
        ResponseEntity<List<Cuenta>> r = cuentaController.getAllCuentas();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(2, r.getBody().size());
    }

    @Test @DisplayName("TC-CTA-02: Listar cuentas sin registros lanza 404")
    void getAllCuentas_sinCuentas_lanza404() {
        when(cuentaRepository.findAll()).thenReturn(Collections.emptyList());
        assertThrows(ResponseStatusException.class, () -> cuentaController.getAllCuentas());
    }

    @Test @DisplayName("TC-CTA-03: Buscar cuenta activa del propio cliente retorna 200")
    void getCuenta_activaDelPropioCliente_retorna200() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaActiva));
        ResponseEntity<Cuenta> r = cuentaController.getCuenta("1234567890", principal);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("1234567890", r.getBody().getNumeroCuenta());
    }

    @Test @DisplayName("TC-CTA-04: Buscar cuenta inexistente lanza 404")
    void getCuenta_noExiste_lanza404() {
        when(cuentaRepository.findByNumeroCuenta(anyString())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> cuentaController.getCuenta("9999999999", principal));
    }

    @Test @DisplayName("TC-CTA-05: Buscar cuenta inactiva lanza 410")
    void getCuenta_inactiva_lanza410() {
        when(cuentaRepository.findByNumeroCuenta("0987654321")).thenReturn(Optional.of(cuentaInactiva));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cuentaController.getCuenta("0987654321", principal));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CTA-06: Buscar cuenta de otro cliente lanza 403")
    void getCuenta_deOtroCliente_lanza403() {
        Cliente otroCliente = new Cliente();
        otroCliente.setId(99L);
        Cuenta cuentaAjena = new Cuenta();
        cuentaAjena.setNumeroCuenta("5555555555");
        cuentaAjena.setCliente(otroCliente);
        cuentaAjena.setActivo(true);
        when(cuentaRepository.findByNumeroCuenta("5555555555")).thenReturn(Optional.of(cuentaAjena));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cuentaController.getCuenta("5555555555", principal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CTA-07: Dar de baja cuenta activa propia retorna 200")
    void bajaLogicaCuenta_activaPropia_retorna200() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.save(any())).thenReturn(cuentaActiva);
        ResponseEntity<Map<String, String>> r = cuentaController.bajaLogicaCuenta("1234567890", principal);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertFalse(cuentaActiva.getActivo());
    }

    @Test @DisplayName("TC-CTA-08: Dar de baja cuenta ya inactiva lanza 409")
    void bajaLogicaCuenta_yaInactiva_lanzaConflicto() {
        when(cuentaRepository.findByNumeroCuenta("0987654321")).thenReturn(Optional.of(cuentaInactiva));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cuentaController.bajaLogicaCuenta("0987654321", principal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CTA-09: Dar de baja cuenta ajena lanza 403")
    void bajaLogicaCuenta_cuentaAjena_lanza403() {
        Cliente otro = new Cliente();
        otro.setId(99L);
        Cuenta ajena = new Cuenta();
        ajena.setNumeroCuenta("5555555555");
        ajena.setCliente(otro);
        ajena.setActivo(true);
        when(cuentaRepository.findByNumeroCuenta("5555555555")).thenReturn(Optional.of(ajena));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cuentaController.bajaLogicaCuenta("5555555555", principal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CTA-10: Dar de baja cuenta inexistente lanza 404")
    void bajaLogicaCuenta_noExiste_lanza404() {
        when(cuentaRepository.findByNumeroCuenta(anyString())).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cuentaController.bajaLogicaCuenta("0000000000", principal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CTA-11: Crear nueva cuenta para cliente activo retorna 201")
    void crearCuenta_clienteActivo_retorna201() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ResponseEntity<Cuenta> r = cuentaController.crearCuenta(principal);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertNotNull(r.getBody().getNumeroCuenta());
        assertEquals(new BigDecimal("0.00"), r.getBody().getSaldo());
    }

    @Test @DisplayName("TC-CTA-12: Crear cuenta para cliente inactivo lanza 403")
    void crearCuenta_clienteInactivo_lanza403() {
        clienteActivo.setActivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cuentaController.crearCuenta(principal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CTA-13: Crear cuenta numero unico generado es de 10 digitos")
    void crearCuenta_numeroCuentaGenerado_es10Digitos() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Cuenta nuevaCuenta = cuentaController.crearCuenta(principal).getBody();
        assertEquals(10, nuevaCuenta.getNumeroCuenta().length());
        assertTrue(nuevaCuenta.getNumeroCuenta().matches("\\d{10}"));
    }
}