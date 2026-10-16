package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.exception.RecursoInactivoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.cliente.ClienteBusquedaDTO;
import com.proyecto.servicios.model.request.cliente.ClienteGestionDTO;
import com.proyecto.servicios.model.request.cliente.ClienteIdentificadorDTO;
import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.ClienteResponseDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Cliente - ClienteController")
class ClienteControllerTest {

    @Mock private OnboardingService onboardingService;
    @Mock private ClienteService clienteService;
    @InjectMocks private ClienteController clienteController;

    private ClienteResponseDTO clienteActivoResp;
    private ClienteRegistroDTO registroDTO;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        clienteActivoResp = ClienteResponseDTO.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .correo("juan@test.com")
                .rfc("PERJ901212ABC")
                .curp("PERJ901212HDFRRL01")
                .activo(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registroDTO = new ClienteRegistroDTO();

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCliente(cliente);
        usuario.setActivo(true);
        principal = new CustomUserDetails(usuario);
    }

    @Test @DisplayName("TC-CLI-01: Registro exitoso retorna 201 con GenericResponse codigo 0")
    void registrarCliente_exitoso_retorna201() {
        when(onboardingService.registrarCliente(any())).thenReturn("1234567890");
        ResponseEntity<GenericResponse<Map<String, String>>> response = clienteController.registrarCliente(registroDTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals("1234567890", response.getBody().getData().get("numero_cuenta"));
    }

    @Test @DisplayName("TC-CLI-02: Registro llama al servicio de onboarding exactamente una vez")
    void registrarCliente_llamaServicioOnboarding() {
        when(onboardingService.registrarCliente(any())).thenReturn("0987654321");
        clienteController.registrarCliente(registroDTO);
        verify(onboardingService, times(1)).registrarCliente(any(ClienteRegistroDTO.class));
    }

    @Test @DisplayName("TC-CLI-03: Consulta y busqueda unica combinada retorna pagina en GenericResponse")
    void consultarClientes_retornaPagina() {
        Page<ClienteResponseDTO> page = new PageImpl<>(List.of(clienteActivoResp));
        ClienteBusquedaDTO filtros = new ClienteBusquedaDTO();
        filtros.setCriterio("Juan");
        when(clienteService.buscar(any(ClienteBusquedaDTO.class))).thenReturn(page);

        ResponseEntity<GenericResponse<Page<ClienteResponseDTO>>> r = clienteController.consultarClientes(filtros);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals(1, r.getBody().getData().getTotalElements());
    }

    @Test @DisplayName("TC-CLI-04: Consulta de detalle por identificador sin @PathVariable retorna 200 con codigo 0")
    void getClienteDetalle_exitoso() {
        ClienteIdentificadorDTO idDto = new ClienteIdentificadorDTO();
        idDto.setRfc("PERJ901212ABC");
        when(clienteService.buscarPorIdentificador(any())).thenReturn(clienteActivoResp);

        ResponseEntity<GenericResponse<ClienteResponseDTO>> r = clienteController.getClienteDetalle(idDto);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals("Juan", r.getBody().getData().getNombre());
    }

    @Test @DisplayName("TC-CLI-05: Consulta de detalle no encontrado lanza RecursoNoEncontradoException")
    void getClienteDetalle_noExiste_lanzaExcepcion() {
        ClienteIdentificadorDTO idDto = new ClienteIdentificadorDTO();
        idDto.setId(99L);
        when(clienteService.buscarPorIdentificador(any())).thenThrow(new RecursoNoEncontradoException("Cliente no encontrado"));

        assertThrows(RecursoNoEncontradoException.class, () -> clienteController.getClienteDetalle(idDto));
    }

    @Test @DisplayName("TC-CLI-06: Consulta de detalle inactivo lanza RecursoInactivoException")
    void getClienteDetalle_inactivo_lanzaExcepcion() {
        ClienteIdentificadorDTO idDto = new ClienteIdentificadorDTO();
        idDto.setId(2L);
        when(clienteService.buscarPorIdentificador(any())).thenThrow(new RecursoInactivoException("El cliente ha sido dado de baja."));

        assertThrows(RecursoInactivoException.class, () -> clienteController.getClienteDetalle(idDto));
    }

    @Test @DisplayName("TC-CLI-07: Consulta de cuentas sin @PathVariable retorna lista en GenericResponse")
    void getCuentasDelCliente_exitoso() {
        CuentaResponseDTO ctaDto = CuentaResponseDTO.builder().numeroCuenta("1234567890").build();
        ClienteIdentificadorDTO idDto = new ClienteIdentificadorDTO();
        idDto.setId(1L);
        when(clienteService.obtenerCuentas(any(), any())).thenReturn(List.of(ctaDto));

        ResponseEntity<GenericResponse<List<CuentaResponseDTO>>> r = clienteController.getCuentasDelCliente(idDto, principal);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals(1, r.getBody().getData().size());
    }

    @Test @DisplayName("TC-CLI-08: Gestion sin @PathVariable con bandera booleana retorna GenericResponse con codigo 0")
    void gestionCliente_exitoso() {
        ClienteGestionDTO dto = new ClienteGestionDTO();
        dto.setRfc("PERJ901212ABC");
        dto.setDesactivar(false);
        when(clienteService.procesarGestionCliente(any())).thenReturn(clienteActivoResp);

        ResponseEntity<GenericResponse<ClienteResponseDTO>> r = clienteController.gestionCliente(dto);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals("Juan", r.getBody().getData().getNombre());
    }
}
