package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.model.AuthRequest;
import com.proyecto.servicios.model.AuthResponse;
import com.proyecto.servicios.model.PasswordChangeDTO;
import com.proyecto.servicios.model.RegisterRequest;
import com.proyecto.servicios.repositorys.UsuarioRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/auth/debug_login")
    public ResponseEntity<Map<String, Object>> debugLogin(@RequestParam String correo, @RequestParam String pass) {
        java.util.Map<String, Object> debug = new java.util.HashMap<>();
        debug.put("correoRecibido", correo);
        debug.put("passRecibido", pass);

        Usuario u = usuarioRepository.findByClienteCorreo(correo).orElse(null);
        debug.put("usuarioEncontrado", u != null);
        if (u != null) {
            debug.put("hashEnBD", u.getPassword());
            debug.put("matches", passwordEncoder.matches(pass, u.getPassword()));
        }
        return ResponseEntity.ok(debug);
    }

    @GetMapping("/auth/debug")
    public ResponseEntity<Boolean> debugBcrypt(@RequestParam String raw) {
        String dbHash = "$2a$10$dZGRfu6pV1gMklWpqKXS3unY5gyeP1dzSaeSsEL72yr7ttcu/oGoO";
        return ResponseEntity.ok(passwordEncoder.matches(raw, dbHash));
    }

    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = usuarioService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/auth/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = usuarioService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Endpoint de lista completa (Restaurado por peticion del profesor)
    @GetMapping("/usuarios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Usuario>> getAllUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        if (usuarios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay usuarios registrados en el sistema.");
        }
        return ResponseEntity.ok(usuarios);
    }

    @PatchMapping("/usuarios/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @Valid @RequestBody PasswordChangeDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña y su confirmación no coinciden.");
        }

        Usuario usuario = usuarioRepository.findByClienteCorreo(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual es incorrecta.");
        }

        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La nueva contraseña no puede ser igual a la contraseña actual.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente."));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("#id == principal.usuarioId")
    public ResponseEntity<Map<String, String>> bajaLogicaUsuario(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!usuario.getActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya estaba dado de baja.");
        }

        usuario.setActivo(false);
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Usuario dado de baja correctamente."));
    }
}
