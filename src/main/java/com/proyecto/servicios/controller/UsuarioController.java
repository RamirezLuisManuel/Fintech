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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(GenericResponse.exito("Inicio de sesion exitoso", usuarioService.login(request)));
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
}