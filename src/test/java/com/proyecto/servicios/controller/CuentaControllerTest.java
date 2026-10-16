package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.cuenta.CuentaIdentificadorDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.service.CuentaService;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Cuenta - CuentaController")
class CuentaControllerTest {

    @Mock private CuentaService cuentaService;
    @InjectMocks private CuentaController cuentaController;

    private CuentaResponseDTO cuentaActivaDto;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        cuentaActivaDto = CuentaResponseDTO.builder()
                .id(1L)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("1000.00"))
                .activo(true)
                .clienteId(1L)
                .clienteNombre("Juan Perez")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCliente(cliente);
        usuario.setActivo(true);
        principal = new CustomUserDetails(usuario);
    }

    @Test @DisplayName("TC-CTA-01: Listar todas las cuentas retorna GenericResponse con lista y codigo 0")
    void getAllCuentas_retornaLista() {
        when(cuentaService.listarTodas()).thenReturn(List.of(cuentaActivaDto));
        ResponseEntity<GenericResponse<List<CuentaResponseDTO>>> r = cuentaController.getAllCuentas();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals(1, r.getBody().getData().size());
    }

    @Test @DisplayName("TC-CTA-02: Consulta de detalle sin @PathVariable retorna GenericResponse con codigo 0")
    void getCuenta_exitoso() {
        CuentaIdentificadorDTO idDto = new CuentaIdentificadorDTO();
        idDto.setNumeroCuenta("1234567890");
        when(cuentaService.buscarPorNumero(any(), eq(1L))).thenReturn(cuentaActivaDto);

        ResponseEntity<GenericResponse<CuentaResponseDTO>> r = cuentaController.getCuenta(idDto, principal);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals("1234567890", r.getBody().getData().getNumeroCuenta());
    }

    @Test @DisplayName("TC-CTA-03: Baja logica sin @PathVariable retorna GenericResponse con codigo 0")
    void bajaLogicaCuenta_exitoso() {
        CuentaIdentificadorDTO idDto = new CuentaIdentificadorDTO();
        idDto.setNumeroCuenta("1234567890");
        doNothing().when(cuentaService).darDeBaja(any(), eq(1L));

        ResponseEntity<GenericResponse<Map<String, String>>> r = cuentaController.bajaLogicaCuenta(idDto, principal);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        verify(cuentaService).darDeBaja(any(), eq(1L));
    }

    @Test @DisplayName("TC-CTA-04: Crear cuenta retorna 201 y GenericResponse con codigo 0")
    void crearCuenta_exitoso() {
        when(cuentaService.crearCuenta(1L)).thenReturn(cuentaActivaDto);
        ResponseEntity<GenericResponse<CuentaResponseDTO>> r = cuentaController.crearCuenta(principal);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertNotNull(r.getBody().getData().getNumeroCuenta());
    }
}
