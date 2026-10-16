package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.auth.AuthRequest;
import com.proyecto.servicios.model.response.AuthResponse;
import com.proyecto.servicios.model.request.auth.PasswordChangeDTO;
import com.proyecto.servicios.model.request.auth.UsuarioIdentificadorDTO;
import com.proyecto.servicios.model.response.UsuarioResponseDTO;
import com.proyecto.servicios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Usuario - UsuarioController")
class UsuarioControllerTest {

    @Mock private UsuarioService usuarioService;
    @InjectMocks private UsuarioController usuarioController;

    private UsuarioResponseDTO usuarioActivoDto;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        usuarioActivoDto = UsuarioResponseDTO.builder()
                .id(1L)
                .correo("juan@test.com")
                .rol("CLIENTE")
                .activo(true)
                .clienteId(1L)
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

    @Test @DisplayName("TC-USR-01: Login exitoso retorna 200 con GenericResponse codigo 0")
    void login_exitoso_retorna200ConToken() {
        AuthRequest req = new AuthRequest();
        req.setCorreo("juan@test.com");
        req.setPassword("Password123#");
        AuthResponse res = new AuthResponse("access_jwt_token", "refresh_jwt_token", "1234567890");
        when(usuarioService.login(any())).thenReturn(res);

        ResponseEntity<GenericResponse<AuthResponse>> r = usuarioController.login(req);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals("access_jwt_token", r.getBody().getData().getAccessToken());
    }

    @Test @DisplayName("TC-USR-02: Listar todos los usuarios retorna lista en GenericResponse con codigo 0")
    void getAllUsuarios_retornaLista() {
        when(usuarioService.listarTodos()).thenReturn(List.of(usuarioActivoDto));
        ResponseEntity<GenericResponse<List<UsuarioResponseDTO>>> r = usuarioController.getAllUsuarios();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        assertEquals(1, r.getBody().getData().size());
        assertEquals("juan@test.com", r.getBody().getData().get(0).getCorreo());
    }

    @Test @DisplayName("TC-USR-03: Cambiar contrasena exitosamente retorna GenericResponse con codigo 0")
    void cambiarPassword_exitoso() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setPasswordActual("Segura123#");
        dto.setPasswordNueva("Nueva456$");
        dto.setPasswordConfirmacion("Nueva456$");
        doNothing().when(usuarioService).cambiarPassword(any(), any());

        ResponseEntity<GenericResponse<Map<String, String>>> r = usuarioController.cambiarPassword(dto, principal);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        verify(usuarioService).cambiarPassword(any(), any());
    }

    @Test @DisplayName("TC-USR-04: Baja logica sin @PathVariable retorna GenericResponse con codigo 0")
    void bajaLogicaUsuario_exitoso() {
        UsuarioIdentificadorDTO idDto = new UsuarioIdentificadorDTO();
        idDto.setId(1L);
        doNothing().when(usuarioService).bajaLogica(any());

        ResponseEntity<GenericResponse<Map<String, String>>> r = usuarioController.bajaLogicaUsuario(idDto);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertEquals(0, r.getBody().getCodigo());
        verify(usuarioService).bajaLogica(any());
    }
}
