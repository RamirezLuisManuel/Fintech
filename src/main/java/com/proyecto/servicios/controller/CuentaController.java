package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.cuenta.CuentaIdentificadorDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping("/consulta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<List<CuentaResponseDTO>>> getAllCuentas() {
        return ResponseEntity.ok(GenericResponse.exito(cuentaService.listarTodas()));
    }

    @PostMapping("/detalle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<CuentaResponseDTO>> getCuenta(
            @Valid @RequestBody CuentaIdentificadorDTO idDto,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(GenericResponse.exito(cuentaService.buscarPorNumero(idDto, principal.getClienteId())));
    }

    @PatchMapping("/baja")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<Map<String, String>>> bajaLogicaCuenta(
            @Valid @RequestBody CuentaIdentificadorDTO idDto,
            @AuthenticationPrincipal CustomUserDetails principal) {
        cuentaService.darDeBaja(idDto, principal.getClienteId());
        return ResponseEntity.ok(GenericResponse.exito("Cuenta dada de baja correctamente.", Map.of("mensaje", "Cuenta dada de baja correctamente.")));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<CuentaResponseDTO>> crearCuenta(@AuthenticationPrincipal CustomUserDetails principal) {
        return new ResponseEntity<>(GenericResponse.exito("Cuenta creada exitosamente", cuentaService.crearCuenta(principal.getClienteId())), HttpStatus.CREATED);
    }
}
