package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.model.AuthRequest;
import com.proyecto.servicios.model.AuthResponse;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Usuario - UsuarioServiceImpl")
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtService jwtService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioServiceImpl usuarioService;

    private Usuario usuarioActivo;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setCorreo("juan@test.com");

        usuarioActivo = new Usuario();
        usuarioActivo.setId(1L);
        usuarioActivo.setCliente(cliente);
        usuarioActivo.setPassword("$2a$10$hashedPassword");
        usuarioActivo.setRol("CLIENTE");
        usuarioActivo.setActivo(true);

        authRequest = new AuthRequest();
        authRequest.setCorreo("juan@test.com");
        authRequest.setPassword("Segura123#");
    }

    @Test @DisplayName("TC-USRSVC-01: Login exitoso retorna tokens JWT")
    void login_exitoso_retornaTokens() {
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Segura123#", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generateToken("juan@test.com")).thenReturn("access_token");
        when(jwtService.generateRefreshToken("juan@test.com")).thenReturn("refresh_token");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(usuarioRepository.save(any())).thenReturn(usuarioActivo);
        AuthResponse r = usuarioService.login(authRequest);
        assertNotNull(r);
        assertEquals("access_token", r.getAccessToken());
        assertEquals("refresh_token", r.getRefreshToken());
    }

    @Test @DisplayName("TC-USRSVC-02: Login falla si usuario no existe")
    void login_usuarioNoExiste_lanzaUnauthorized() {
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> usuarioService.login(authRequest));
    }

    @Test @DisplayName("TC-USRSVC-03: Login falla si contrasena es incorrecta")
    void login_contrasenaIncorrecta_lanzaUnauthorized() {
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Segura123#", "$2a$10$hashedPassword")).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> usuarioService.login(authRequest));
    }

    @Test @DisplayName("TC-USRSVC-04: Login exitoso guarda refresh token en BD")
    void login_exitoso_guardaRefreshTokenEnBD() {
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString())).thenReturn("access");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(usuarioRepository.save(any())).thenReturn(usuarioActivo);
        usuarioService.login(authRequest);
        verify(usuarioRepository).save(argThat(u -> "refresh".equals(u.getRefreshToken())));
    }

    @Test @DisplayName("TC-USRSVC-05: Login exitoso elimina sesion previa de Redis")
    void login_exitoso_eliminaSesionPreviaDeRedis() {
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString())).thenReturn("access");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(usuarioRepository.save(any())).thenReturn(usuarioActivo);
        usuarioService.login(authRequest);
        verify(redisTemplate).delete("user_session:juan@test.com");
    }

    @Test @DisplayName("TC-USRSVC-06: Login exitoso guarda nueva sesion en Redis")
    void login_exitoso_guardaSesionEnRedis() {
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString())).thenReturn("access");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(usuarioRepository.save(any())).thenReturn(usuarioActivo);
        usuarioService.login(authRequest);
        verify(valueOps).set("user_session:juan@test.com", "refresh");
    }

    @Test @DisplayName("TC-USRSVC-07: Login falla si el usuario esta dado de baja")
    void login_usuarioInactivo_lanzaForbidden() {
        usuarioActivo.setActivo(false);
        when(usuarioRepository.findByClienteCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> usuarioService.login(authRequest));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("El usuario ha sido dado de baja", ex.getReason());
    }
}