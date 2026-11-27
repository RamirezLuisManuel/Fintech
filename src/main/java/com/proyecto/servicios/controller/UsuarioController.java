package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.auth.AuthRequest;
import com.proyecto.servicios.model.response.AuthResponse;
import com.proyecto.servicios.model.request.auth.PasswordChangeDTO;
import com.proyecto.servicios.model.request.auth.PinChangeDTO;
import com.proyecto.servicios.model.request.auth.RegisterRequest;
import com.proyecto.servicios.model.request.auth.UsuarioIdentificadorDTO;
import com.proyecto.servicios.model.response.UsuarioResponseDTO;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    @org.springframework.beans.factory.annotation.Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Inicio de sesion exitoso", usuarioService.login(request)));
    }

    @PostMapping(value = "/auth/pre-login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse<AuthResponse>> preLogin(@Valid @RequestBody AuthRequest request) {
        AuthResponse res = usuarioService.preLogin(request);
        String msg = Boolean.TRUE.equals(res.getActiveSessionDetected())
                ? "Sesion activa detectada en otro dispositivo"
                : "Credenciales verificadas, procede con segundo factor (2FA)";
        return ResponseEntity.ok(GenericResponse.exito(msg, res));
    }

    @PostMapping(value = "/auth/verify-2fa", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse<AuthResponse>> verify2fa(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Autenticacion de doble factor exitosa", usuarioService.verify2faAndLogin(request)));
    }

    @PostMapping(value = "/auth/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        String correo = body != null ? body.get("correo") : null;
        String refreshToken = body != null ? body.get("refreshToken") : null;
        return ResponseEntity.ok(GenericResponse.exito("Token renovado exitosamente", usuarioService.refreshSession(correo, refreshToken)));
    }

    @PostMapping(value = "/auth/logout")
    public ResponseEntity<GenericResponse<Map<String, String>>> logout(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody(required = false) Map<String, String> body) {
        String username = principal != null ? principal.getUsername() : (body != null ? body.get("correo") : null);
        usuarioService.logout(username);
        return ResponseEntity.ok(GenericResponse.exito("Sesion cerrada exitosamente", Map.of("mensaje", "Sesion cerrada exitosamente")));
    }

    @PostMapping("/usuarios/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<Map<String, Object>>> getPerfilActual(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(GenericResponse.exito("Perfil obtenido exitosamente", usuarioService.obtenerPerfilActual(principal.getUsername())));
    }

    @PostMapping(value = "/auth/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(GenericResponse.exito("Usuario registrado exitosamente", usuarioService.register(request)), HttpStatus.CREATED);
    }

    @PostMapping("/usuarios/consulta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<List<UsuarioResponseDTO>>> getAllUsuarios() {
        return ResponseEntity.ok(GenericResponse.exito(usuarioService.listarTodos()));
    }

    @PatchMapping("/usuarios/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<Map<String, String>>> cambiarPassword(
            @Valid @RequestBody PasswordChangeDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        usuarioService.cambiarPassword(principal.getUsername(), request);
        return ResponseEntity.ok(GenericResponse.exito("Contrasena actualizada exitosamente.", Map.of("mensaje", "Contrasena actualizada exitosamente.")));
    }

    @PatchMapping("/usuarios/baja")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<Map<String, String>>> bajaLogicaUsuario(
            @Valid @RequestBody UsuarioIdentificadorDTO idDto) {
        usuarioService.bajaLogica(idDto);
        return ResponseEntity.ok(GenericResponse.exito("Usuario dado de baja correctamente.", Map.of("mensaje", "Usuario dado de baja correctamente.")));
    }
    @PatchMapping("/usuarios/pin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<Map<String, String>>> actualizarPin(
            @Valid @RequestBody PinChangeDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        usuarioService.actualizarPin(principal.getUsername(), request);
        return ResponseEntity.ok(GenericResponse.exito("PIN de seguridad actualizado exitosamente.", Map.of("mensaje", "PIN de seguridad actualizado exitosamente.")));
    }

    @PostMapping("/usuarios/verificar-pin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<Map<String, Boolean>>> verificarPin(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal) {
        String pin = body != null ? body.get("pin") : null;
        if (pin == null || !pin.matches("^\\d{4}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GenericResponse.errorValidacion("El PIN debe contener exactamente 4 digitos."));
        }
        String storedPinHash = principal.getUsuario().getPin();
        boolean valid = storedPinHash != null && passwordEncoder.matches(pin, storedPinHash);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GenericResponse.errorNegocio("PIN de seguridad incorrecto."));
        }
        return ResponseEntity.ok(GenericResponse.exito("PIN verificado correctamente.", Map.of("valido", true)));
    }
}