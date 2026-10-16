package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.CustomUserDetails;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.request.cliente.ClienteBusquedaDTO;
import com.proyecto.servicios.model.request.cliente.ClienteGestionDTO;
import com.proyecto.servicios.model.request.cliente.ClienteIdentificadorDTO;
import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.ClienteResponseDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final OnboardingService onboardingService;
    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<GenericResponse<Map<String, String>>> registrarCliente(@Valid @RequestBody ClienteRegistroDTO request) {
        String numCuenta = onboardingService.registrarCliente(request);
        Map<String, String> data = Map.of("mensaje", "Cliente registrado exitosamente", "numero_cuenta", numCuenta);
        return new ResponseEntity<>(GenericResponse.exito("Cliente registrado exitosamente", data), HttpStatus.CREATED);
    }

    @PostMapping("/consulta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<Page<ClienteResponseDTO>>> consultarClientes(
            @Valid @RequestBody ClienteBusquedaDTO filtros) {
        return ResponseEntity.ok(GenericResponse.exito(clienteService.buscar(filtros)));
    }

    @PostMapping("/detalle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> getClienteDetalle(
            @Valid @RequestBody ClienteIdentificadorDTO idDto) {
        return ResponseEntity.ok(GenericResponse.exito(clienteService.buscarPorIdentificador(idDto)));
    }

    @PostMapping("/cuentas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<List<CuentaResponseDTO>>> getCuentasDelCliente(
            @Valid @RequestBody ClienteIdentificadorDTO idDto,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long clienteId = principal != null ? principal.getClienteId() : null;
        return ResponseEntity.ok(GenericResponse.exito(clienteService.obtenerCuentas(idDto, clienteId)));
    }

    @PatchMapping("/gestion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericResponse<ClienteResponseDTO>> gestionCliente(
            @Valid @RequestBody ClienteGestionDTO request) {
        ClienteResponseDTO dto = clienteService.procesarGestionCliente(request);
        String msg = Boolean.TRUE.equals(request.getDesactivar()) ? "Cliente dado de baja exitosamente" : "Cliente actualizado exitosamente";
        return ResponseEntity.ok(GenericResponse.exito(msg, dto));
    }
}
