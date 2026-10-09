package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.model.AuthRequest;
import com.proyecto.servicios.model.AuthResponse;
import com.proyecto.servicios.model.PasswordChangeDTO;
import com.proyecto.servicios.repositorys.UsuarioRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Usuario - UsuarioController")
class UsuarioControllerTest {

    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioController usuarioController;

    private Usuario usuarioActivo;
    private Usuario usuarioInactivo;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setCorreo("juan@test.com");

        usuarioActivo = new Usuario();
        usuarioActivo.setId(1L);
        usuarioActivo.setCliente(cliente);
        usuarioActivo.setPassword("$2a$10$hashedActual");
        usuarioActivo.setRol("CLIENTE");
        usuarioActivo.setActivo(true);
        usuarioActivo.setUpdatedAt(LocalDateTime.now());

        usuarioInactivo = new Usuario();
        usuarioInactivo.setId(2L);
        usuarioInactivo.setCliente(cliente);
        usuarioInactivo.setPassword("$2a$10$hashX");
        usuarioInactivo.setActivo(false);
        usuarioInactivo.setUpdatedAt(LocalDateTime.now());

        principal = new CustomUserDetails(usuarioActivo);
    }

    @Test @DisplayName("TC-USR-01: Login exitoso retorna 200 con token")
    void login_exitoso_retorna200() {
        AuthRequest req = new AuthRequest();
        req.setCorreo("juan@test.com");
        req.setPassword("Segura123#");
        AuthResponse res = new AuthResponse("access_token", "refresh_token", "1234567890");
        when(usuarioService.login(any())).thenReturn(res);
        ResponseEntity<AuthResponse> r = usuarioController.login(req);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("access_token", r.getBody().getAccessToken());
    }

    @Test @DisplayName("TC-USR-02: Login llama al servicio de usuario")
    void login_llamaServicioDeUsuario() {
        AuthRequest req = new AuthRequest();
        req.setCorreo("juan@test.com");
        req.setPassword("Segura123#");
        when(usuarioService.login(any())).thenReturn(new AuthResponse("t", "r", "c"));
        usuarioController.login(req);
        verify(usuarioService, times(1)).login(any(AuthRequest.class));
    }

    @Test @DisplayName("TC-USR-03: Listar todos los usuarios retorna lista")
    void getAllUsuarios_retornaLista() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioActivo, usuarioInactivo));
        ResponseEntity<List<Usuario>> r = usuarioController.getAllUsuarios();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(2, r.getBody().size());
    }

    @Test @DisplayName("TC-USR-04: Listar usuarios cuando no hay registros lanza 404")
    void getAllUsuarios_sinRegistros_lanza404() {
        when(usuarioRepository.findAll()).thenReturn(Collections.emptyList());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioController.getAllUsuarios());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test @DisplayName("TC-USR-05: Cambiar contrasena exitosamente retorna 200")
    void cambiarPassword_exitoso_retorna200() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setPasswordActual("Segura123#");
        dto.setPasswordNueva("Nueva456$");
        dto.setPasswordConfirmacion("Nueva456$");
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Segura123#", "$2a$10$hashedActual")).thenReturn(true);
        when(passwordEncoder.matches("Nueva456$", "$2a$10$hashedActual")).thenReturn(false);
        when(passwordEncoder.encode("Nueva456$")).thenReturn("$2a$10$hashedNueva");
        when(usuarioRepository.save(any())).thenReturn(usuarioActivo);
        ResponseEntity<Map<String, String>> r = usuarioController.cambiarPassword(dto, principal);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test @DisplayName("TC-USR-06: Cambiar contrasena falla si confirmacion no coincide")
    void cambiarPassword_confirmacionNoCoincide_lanzaBadRequest() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setPasswordActual("Segura123#");
        dto.setPasswordNueva("Nueva456$");
        dto.setPasswordConfirmacion("Diferente789$");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioController.cambiarPassword(dto, principal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test @DisplayName("TC-USR-07: Cambiar contrasena falla si contrasena actual es incorrecta")
    void cambiarPassword_actualIncorrecta_lanzaUnauthorized() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setPasswordActual("Incorrecta999$");
        dto.setPasswordNueva("Nueva456$");
        dto.setPasswordConfirmacion("Nueva456$");
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Incorrecta999$", "$2a$10$hashedActual")).thenReturn(false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioController.cambiarPassword(dto, principal));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test @DisplayName("TC-USR-08: Cambiar contrasena falla si nueva es igual a la actual")
    void cambiarPassword_nuevaIgualAActual_lanzaConflicto() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setPasswordActual("Segura123#");
        dto.setPasswordNueva("Segura123#");
        dto.setPasswordConfirmacion("Segura123#");
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Segura123#", "$2a$10$hashedActual")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioController.cambiarPassword(dto, principal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test @DisplayName("TC-USR-09: Cambiar contrasena codifica la nueva con BCrypt")
    void cambiarPassword_exitoso_codificaNuevaConBcrypt() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setPasswordActual("Segura123#");
        dto.setPasswordNueva("Nueva456$");
        dto.setPasswordConfirmacion("Nueva456$");
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Segura123#", "$2a$10$hashedActual")).thenReturn(true);
        when(passwordEncoder.matches("Nueva456$", "$2a$10$hashedActual")).thenReturn(false);
        when(passwordEncoder.encode("Nueva456$")).thenReturn("$2a$10$hashedNueva");
        when(usuarioRepository.save(any())).thenReturn(usuarioActivo);
        usuarioController.cambiarPassword(dto, principal);
        verify(passwordEncoder, times(1)).encode("Nueva456$");
    }

    @Test @DisplayName("TC-USR-10: Baja logica de usuario activo retorna 200 y lo marca inactivo")
    void bajaLogicaUsuario_activo_retorna200YMarcaInactivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(usuarioRepository.save(any())).thenReturn(usuarioActivo);
        ResponseEntity<Map<String, String>> r = usuarioController.bajaLogicaUsuario(1L);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertFalse(usuarioActivo.getActivo());
    }

    @Test @DisplayName("TC-USR-11: Baja logica de usuario ya inactivo lanza 409")
    void bajaLogicaUsuario_yaInactivo_lanzaConflicto() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioInactivo));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioController.bajaLogicaUsuario(2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test @DisplayName("TC-USR-12: Baja logica de usuario inexistente lanza 404")
    void bajaLogicaUsuario_noExiste_lanza404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioController.bajaLogicaUsuario(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}