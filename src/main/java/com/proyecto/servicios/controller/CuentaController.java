package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.repositorys.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaRepository cuentaRepository;
    private final com.proyecto.servicios.repositorys.ClienteRepository clienteRepository;

    // Endpoint de lista completa (Restaurado por peticion del profesor)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cuenta>> getAllCuentas() {
        List<Cuenta> cuentas = cuentaRepository.findAll();
        if (cuentas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay cuentas registradas en el sistema.");
        }
        return ResponseEntity.ok(cuentas);
    }

    @GetMapping("/{numeroCuenta}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cuenta> getCuenta(
            @PathVariable String numeroCuenta,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        
        if (!cuenta.getCliente().getId().equals(principal.getClienteId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta cuenta.");
        }
        if (!cuenta.getActivo()) {
            throw new ResponseStatusException(HttpStatus.GONE, "La cuenta está desactivada.");
        }
        
        return ResponseEntity.ok(cuenta);
    }

    @DeleteMapping("/{numeroCuenta}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> bajaLogicaCuenta(
            @PathVariable String numeroCuenta,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        
        if (!cuenta.getCliente().getId().equals(principal.getClienteId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta cuenta.");
        }
        if (!cuenta.getActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya estaba dada de baja.");
        }
        
        cuenta.setActivo(false);
        cuenta.setUpdatedAt(LocalDateTime.now());
        cuentaRepository.save(cuenta);
        
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta dada de baja correctamente."));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cuenta> crearCuenta(@AuthenticationPrincipal CustomUserDetails principal) {
        com.proyecto.servicios.entity.Cliente cliente = clienteRepository.findById(principal.getClienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        if (!cliente.getActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El cliente esta inactivo. No puede crear cuentas.");
        }

        Cuenta nuevaCuenta = new Cuenta();
        nuevaCuenta.setCliente(cliente);
        nuevaCuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        nuevaCuenta.setSaldo(new java.math.BigDecimal("0.00"));
        nuevaCuenta.setActivo(true);
        nuevaCuenta.setCreatedAt(LocalDateTime.now());
        nuevaCuenta.setUpdatedAt(LocalDateTime.now());

        return new ResponseEntity<>(cuentaRepository.save(nuevaCuenta), HttpStatus.CREATED);
    }

    private String generarNumeroCuentaUnico() {
        java.util.Random random = new java.util.Random();
        String cuenta;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            cuenta = sb.toString();
        } while (cuentaRepository.existsByNumeroCuenta(cuenta));
        return cuenta;
    }
}
