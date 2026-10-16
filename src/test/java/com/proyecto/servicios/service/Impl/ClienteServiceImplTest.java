package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.exception.DatoDuplicadoException;
import com.proyecto.servicios.exception.RecursoInactivoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.request.cliente.ClienteBusquedaDTO;
import com.proyecto.servicios.model.request.cliente.ClienteGestionDTO;
import com.proyecto.servicios.model.request.cliente.ClienteIdentificadorDTO;
import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.ClienteResponseDTO;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Cliente - ClienteServiceImpl")
class ClienteServiceImplTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private CuentaRepository cuentaRepository;
    @InjectMocks private ClienteServiceImpl clienteService;

    private Cliente clienteActivo;
    private Cliente clienteInactivo;

    @BeforeEach
    void setUp() {
        clienteActivo = new Cliente();
        clienteActivo.setId(1L);
        clienteActivo.setNombre("Carlos");
        clienteActivo.setApellidoPaterno("Hernandez");
        clienteActivo.setCorreo("carlos@test.com");
        clienteActivo.setRfc("HERC901010XYZ");
        clienteActivo.setCurp("HERC901010HDFRR01");
        clienteActivo.setActivo(true);
        clienteActivo.setUpdatedAt(LocalDateTime.now());

        clienteInactivo = new Cliente();
        clienteInactivo.setId(2L);
        clienteInactivo.setNombre("Laura");
        clienteInactivo.setCorreo("laura@test.com");
        clienteInactivo.setActivo(false);
    }

    @Test
    @DisplayName("TC-CLISVC-01: Busqueda combinada con Request DTO retorna pagina de Response DTOs")
    void buscar_conFiltros_retornaPagina() {
        Page<Cliente> page = new PageImpl<>(List.of(clienteActivo));
        when(clienteRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        ClienteBusquedaDTO filtros = new ClienteBusquedaDTO();
        filtros.setCriterio("Carlos");
        Page<ClienteResponseDTO> res = clienteService.buscar(filtros);

        assertNotNull(res);
        assertEquals(1, res.getTotalElements());
        assertEquals("Carlos", res.getContent().get(0).getNombre());
    }

    @Test
    @DisplayName("TC-CLISVC-02: Buscar por identificador exitoso")
    void buscarPorIdentificador_exitoso() {
        ClienteIdentificadorDTO idDto = new ClienteIdentificadorDTO();
        idDto.setRfc("HERC901010XYZ");
        when(clienteRepository.findByRfc("HERC901010XYZ")).thenReturn(Optional.of(clienteActivo));

        ClienteResponseDTO res = clienteService.buscarPorIdentificador(idDto);
        assertNotNull(res);
        assertEquals("Carlos", res.getNombre());
    }

    @Test
    @DisplayName("TC-CLISVC-03: Buscar por identificador inexistente lanza RecursoNoEncontradoException")
    void buscarPorIdentificador_noExiste_lanzaExcepcion() {
        ClienteIdentificadorDTO idDto = new ClienteIdentificadorDTO();
        idDto.setId(99L);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.buscarPorIdentificador(idDto));
    }

    @Test
    @DisplayName("TC-CLISVC-04: Buscar por identificador inactivo lanza RecursoInactivoException")
    void buscarPorIdentificador_inactivo_lanzaExcepcion() {
        ClienteIdentificadorDTO idDto = new ClienteIdentificadorDTO();
        idDto.setId(2L);
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInactivo));

        assertThrows(RecursoInactivoException.class, () -> clienteService.buscarPorIdentificador(idDto));
    }

    @Test
    @DisplayName("TC-CLISVC-05: Gestion sin PathVariable con bandera desactivar=true ejecuta baja logica")
    void procesarGestionCliente_desactivarTrue() {
        ClienteGestionDTO dto = new ClienteGestionDTO();
        dto.setRfc("HERC901010XYZ");
        dto.setDesactivar(true);

        when(clienteRepository.findByRfc("HERC901010XYZ")).thenReturn(Optional.of(clienteActivo));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(Collections.emptyList());
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO res = clienteService.procesarGestionCliente(dto);
        assertFalse(res.getActivo());
    }

    @Test
    @DisplayName("TC-CLISVC-06: Gestion sin PathVariable con bandera desactivar=false actualiza datos")
    void procesarGestionCliente_desactivarFalse_actualiza() {
        ClienteGestionDTO dto = new ClienteGestionDTO();
        dto.setRfc("HERC901010XYZ");
        dto.setDesactivar(false);
        dto.setTelefonoMovil("4189998877");

        when(clienteRepository.findByRfc("HERC901010XYZ")).thenReturn(Optional.of(clienteActivo));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO res = clienteService.procesarGestionCliente(dto);
        assertEquals("4189998877", res.getTelefonoMovil());
        assertTrue(res.getActivo());
    }

    @Test
    @DisplayName("TC-CLISVC-07: Gestion lanza DatoDuplicadoException si correo ya existe")
    void procesarGestionCliente_correoDuplicado_lanzaExcepcion() {
        ClienteGestionDTO dto = new ClienteGestionDTO();
        dto.setRfc("HERC901010XYZ");
        dto.setCorreo("duplicado@test.com");

        when(clienteRepository.findByRfc("HERC901010XYZ")).thenReturn(Optional.of(clienteActivo));
        when(clienteRepository.existsByCorreo("duplicado@test.com")).thenReturn(true);

        assertThrows(DatoDuplicadoException.class, () -> clienteService.procesarGestionCliente(dto));
    }
}