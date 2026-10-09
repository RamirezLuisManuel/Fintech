package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.model.onboarding.ClientePatchDTO;
import com.proyecto.servicios.model.onboarding.ClienteRegistroDTO;
import com.proyecto.servicios.model.onboarding.DomicilioDTO;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.OnboardingService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Cliente - ClienteController")
class ClienteControllerTest {

    @Mock private OnboardingService onboardingService;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CuentaRepository cuentaRepository;
    @InjectMocks private ClienteController clienteController;

    private Cliente clienteActivo;
    private Cliente clienteInactivo;
    private ClienteRegistroDTO registroDTO;

    @BeforeEach
    void setUp() {
        clienteActivo = new Cliente();
        clienteActivo.setId(1L);
        clienteActivo.setNombre("Juan");
        clienteActivo.setApellidoPaterno("Perez");
        clienteActivo.setCorreo("juan@test.com");
        clienteActivo.setRfc("PERJ901212ABC");
        clienteActivo.setCurp("PERJ901212HDFRRL01");
        clienteActivo.setActivo(true);
        clienteActivo.setUpdatedAt(LocalDateTime.now());

        clienteInactivo = new Cliente();
        clienteInactivo.setId(2L);
        clienteInactivo.setNombre("Maria");
        clienteInactivo.setCorreo("maria@test.com");
        clienteInactivo.setActivo(false);

        registroDTO = new ClienteRegistroDTO();
        registroDTO.setNombre("Juan");
        registroDTO.setSegundoNombre("Carlos");
        registroDTO.setApellidoPaterno("Perez");
        registroDTO.setApellidoMaterno("Lopez");
        registroDTO.setCorreo("juan@test.com");
        registroDTO.setPassword("Segura123#");
        registroDTO.setRfc("PERJ901212ABC");
        registroDTO.setCurp("PERJ901212HDFRRL01");
        registroDTO.setSexo("M");
        registroDTO.setNacionalidad("Mexicana");
        registroDTO.setEstadoCivil("Soltero");
        registroDTO.setTelefonoMovil("4151234567");
        registroDTO.setOcupacion("Ing");
        registroDTO.setEmpresa("ACME");
        registroDTO.setIngresoMensual(new BigDecimal("10000"));
        registroDTO.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        DomicilioDTO dom = new DomicilioDTO();
        dom.setCalle("Hidalgo");
        dom.setNumeroExterior("10");
        dom.setColonia("Centro");
        dom.setMunicipio("Leon");
        dom.setEstado("Guanajuato");
        dom.setCodigoPostal("37000");
        dom.setPais("Mexico");
        registroDTO.setDomicilio(dom);
    }

    @Test @DisplayName("TC-CLI-01: Registro exitoso retorna 201 con numero de cuenta")
    void registrarCliente_exitoso_retorna201() {
        when(onboardingService.registrarCliente(any())).thenReturn("1234567890");
        ResponseEntity<Map<String, String>> r = clienteController.registrarCliente(registroDTO);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("1234567890", r.getBody().get("numero_cuenta"));
    }

    @Test @DisplayName("TC-CLI-02: Registro llama al servicio de onboarding exactamente una vez")
    void registrarCliente_llamaOnboardingServiceUnaVez() {
        when(onboardingService.registrarCliente(any())).thenReturn("0987654321");
        clienteController.registrarCliente(registroDTO);
        verify(onboardingService, times(1)).registrarCliente(any(ClienteRegistroDTO.class));
    }

    @Test @DisplayName("TC-CLI-03: Listar todos los clientes retorna lista completa incluyendo inactivos")
    void getClientes_retornaListaCompletaConInactivos() {
        when(clienteRepository.findAll()).thenReturn(Arrays.asList(clienteActivo, clienteInactivo));
        ResponseEntity<List<Cliente>> r = clienteController.getClientes();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(2, r.getBody().size());
    }

    @Test @DisplayName("TC-CLI-04: Listar cuando no hay clientes retorna lista vacia")
    void getClientes_listaVacia_retornaListaVacia() {
        when(clienteRepository.findAll()).thenReturn(Collections.emptyList());
        ResponseEntity<List<Cliente>> r = clienteController.getClientes();
        assertTrue(r.getBody().isEmpty());
    }

    @Test @DisplayName("TC-CLI-05: Buscar cliente activo por ID retorna 200")
    void getClientePorId_activoExistente_retorna200() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        assertEquals(HttpStatus.OK, clienteController.getClientePorId(1L).getStatusCode());
    }

    @Test @DisplayName("TC-CLI-06: Buscar cliente inexistente por ID lanza 404")
    void getClientePorId_noExiste_lanza404() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> clienteController.getClientePorId(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CLI-07: Buscar cliente inactivo por ID lanza 410")
    void getClientePorId_inactivo_lanza410() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInactivo));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> clienteController.getClientePorId(2L));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CLI-08: Buscar cliente activo por RFC retorna 200")
    void getClientePorRfc_activoExistente_retorna200() {
        when(clienteRepository.findByRfc("PERJ901212ABC")).thenReturn(Optional.of(clienteActivo));
        assertEquals(HttpStatus.OK, clienteController.getClientePorRfc("PERJ901212ABC").getStatusCode());
    }

    @Test @DisplayName("TC-CLI-09: Buscar por RFC inexistente lanza 404")
    void getClientePorRfc_noExiste_lanza404() {
        when(clienteRepository.findByRfc(anyString())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> clienteController.getClientePorRfc("XXXXX"));
    }

    @Test @DisplayName("TC-CLI-10: Buscar por RFC inactivo lanza 410")
    void getClientePorRfc_inactivo_lanza410() {
        when(clienteRepository.findByRfc(anyString())).thenReturn(Optional.of(clienteInactivo));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> clienteController.getClientePorRfc("SOME123456ABC"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CLI-11: Buscar cliente activo por CURP retorna 200")
    void getClientePorCurp_activoExistente_retorna200() {
        when(clienteRepository.findByCurp("PERJ901212HDFRRL01")).thenReturn(Optional.of(clienteActivo));
        assertEquals(HttpStatus.OK, clienteController.getClientePorCurp("PERJ901212HDFRRL01").getStatusCode());
    }

    @Test @DisplayName("TC-CLI-12: Buscar por CURP inactivo lanza 410")
    void getClientePorCurp_inactivo_lanza410() {
        when(clienteRepository.findByCurp(anyString())).thenReturn(Optional.of(clienteInactivo));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> clienteController.getClientePorCurp("XXXX901212HDFRRL01"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CLI-13: Obtener cuentas del cliente retorna lista de cuentas")
    void getCuentasDelCliente_conCuentas_retorna200() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setActivo(true);
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuenta));
        ResponseEntity<List<Cuenta>> r = clienteController.getCuentasDelCliente(1L);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(1, r.getBody().size());
    }

    @Test @DisplayName("TC-CLI-14: Obtener cuentas de cliente sin cuentas lanza 404")
    void getCuentasDelCliente_sinCuentas_lanza404() {
        when(cuentaRepository.findByClienteId(1L)).thenReturn(Collections.emptyList());
        assertThrows(ResponseStatusException.class, () -> clienteController.getCuentasDelCliente(1L));
    }

    @Test @DisplayName("TC-CLI-15: Dar de baja cliente activo retorna mensaje y lo marca inactivo")
    void bajaLogicaCliente_activo_retornaMensajeYMarcaInactivo() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(clienteRepository.save(any())).thenReturn(clienteActivo);
        when(cuentaRepository.findByClienteId(1L)).thenReturn(Collections.emptyList());
        ResponseEntity<Map<String, String>> r = clienteController.bajaLogicaCliente(1L);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertFalse(clienteActivo.getActivo());
    }

    @Test @DisplayName("TC-CLI-16: Dar de baja cliente ya inactivo lanza 409")
    void bajaLogicaCliente_yaInactivo_lanzaConflicto() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInactivo));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> clienteController.bajaLogicaCliente(2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CLI-17: Dar de baja cliente desactiva sus cuentas asociadas")
    void bajaLogicaCliente_desactivaCuentasAsociadas() {
        Cuenta cuenta = new Cuenta();
        cuenta.setActivo(true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(clienteRepository.save(any())).thenReturn(clienteActivo);
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuenta));
        when(cuentaRepository.save(any())).thenReturn(cuenta);
        clienteController.bajaLogicaCliente(1L);
        assertFalse(cuenta.getActivo());
        verify(cuentaRepository).save(cuenta);
    }

    @Test @DisplayName("TC-CLI-18: Dar de baja cliente inexistente lanza 404")
    void bajaLogicaCliente_noExiste_lanza404() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> clienteController.bajaLogicaCliente(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test @DisplayName("TC-CLI-19: Actualizacion parcial de cliente activo retorna 200")
    void actualizarClienteParcial_activo_retorna200() {
        ClientePatchDTO patch = new ClientePatchDTO();
        patch.setNombre("NuevoNombre");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(clienteRepository.save(any())).thenReturn(clienteActivo);
        assertEquals(HttpStatus.OK, clienteController.actualizarClienteParcial(1L, patch).getStatusCode());
    }

    @Test @DisplayName("TC-CLI-20: Actualizacion parcial de cliente inactivo lanza 410")
    void actualizarClienteParcial_inactivo_lanza410() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInactivo));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> clienteController.actualizarClienteParcial(2L, new ClientePatchDTO()));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }
}