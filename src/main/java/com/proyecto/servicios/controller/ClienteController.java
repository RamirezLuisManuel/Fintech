package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.model.onboarding.ClientePatchDTO;
import com.proyecto.servicios.model.onboarding.ClienteRegistroDTO;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final OnboardingService onboardingService;
    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;

    @PostMapping
    public ResponseEntity<Map<String, String>> registrarCliente(@Valid @RequestBody ClienteRegistroDTO request) {
        String numCuenta = onboardingService.registrarCliente(request);
        return new ResponseEntity<>(
            Map.of("mensaje", "Cliente registrado exitosamente", "numero_cuenta", numCuenta),
            HttpStatus.CREATED
        );
    }

    // Endpoint de lista completa (Restaurado por peticion del profesor)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cliente>> getClientes() {
        List<Cliente> clientes = clienteRepository.findAll();
        return ResponseEntity.ok(clientes);
    }

    // Endpoint de busqueda por RFC (Restaurado por peticion del profesor)
    @GetMapping("/buscar/rfc/{rfc}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cliente> getClientePorRfc(@PathVariable String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc.toUpperCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró ningún cliente con el RFC: " + rfc));
        if (!cliente.getActivo()) {
            throw new ResponseStatusException(HttpStatus.GONE, "El cliente con RFC " + rfc + " ha sido dado de baja.");
        }
        return ResponseEntity.ok(cliente);
    }

    // Endpoint de busqueda por CURP (Restaurado por peticion del profesor)
    @GetMapping("/buscar/curp/{curp}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cliente> getClientePorCurp(@PathVariable String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp.toUpperCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró ningún cliente con la CURP: " + curp));
        if (!cliente.getActivo()) {
            throw new ResponseStatusException(HttpStatus.GONE, "El cliente con CURP " + curp + " ha sido dado de baja.");
        }
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cliente> getClientePorId(@PathVariable Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        if (!cliente.getActivo()) {
            throw new ResponseStatusException(HttpStatus.GONE, "El cliente ha sido dado de baja.");
        }
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/{id}/cuentas")
    @PreAuthorize("#id == principal.clienteId")
    public ResponseEntity<List<Cuenta>> getCuentasDelCliente(@PathVariable Long id) {
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        if (cuentas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no tiene cuentas registradas.");
        }
        return ResponseEntity.ok(cuentas);
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == principal.clienteId")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteRegistroDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        if (!cliente.getActivo()) {
            throw new ResponseStatusException(HttpStatus.GONE, "No se puede actualizar un cliente dado de baja.");
        }

        if (!cliente.getCorreo().equals(request.getCorreo()) &&
                clienteRepository.existsByCorreo(request.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya está en uso.");
        }

        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreo(request.getCorreo());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("#id == principal.clienteId")
    public ResponseEntity<Cliente> actualizarClienteParcial(@PathVariable Long id, @Valid @RequestBody ClientePatchDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        if (!cliente.getActivo()) {
            throw new ResponseStatusException(HttpStatus.GONE, "No se puede actualizar un cliente dado de baja.");
        }

        if (request.getNombre() != null) cliente.setNombre(request.getNombre());
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(request.getSegundoNombre());
        if (request.getApellidoPaterno() != null) cliente.setApellidoPaterno(request.getApellidoPaterno());
        if (request.getApellidoMaterno() != null) cliente.setApellidoMaterno(request.getApellidoMaterno());
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(request.getEstadoCivil());
        if (request.getTelefonoMovil() != null) cliente.setTelefonoMovil(request.getTelefonoMovil());
        if (request.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        if (request.getOcupacion() != null) cliente.setOcupacion(request.getOcupacion());
        if (request.getEmpresa() != null) cliente.setEmpresa(request.getEmpresa());
        if (request.getIngresoMensual() != null) cliente.setIngresoMensual(request.getIngresoMensual());

        if (request.getCorreo() != null && !request.getCorreo().equals(cliente.getCorreo())) {
            if (clienteRepository.existsByCorreo(request.getCorreo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya está en uso.");
            }
            cliente.setCorreo(request.getCorreo());
        }

        cliente.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("#id == principal.clienteId")
    public ResponseEntity<Map<String, String>> bajaLogicaCliente(@PathVariable Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        if (!cliente.getActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente ya estaba dado de baja.");
        }
        cliente.setActivo(false);
        cliente.setUpdatedAt(LocalDateTime.now());
        clienteRepository.save(cliente);
        
        // Dar de baja tambien todas sus cuentas asociadas
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        for (Cuenta cuenta : cuentas) {
            cuenta.setActivo(false);
            cuenta.setUpdatedAt(LocalDateTime.now());
            cuentaRepository.save(cuenta);
        }
        
        return ResponseEntity.ok(Map.of("mensaje", "Cliente y sus cuentas asociadas dados de baja correctamente (Usuario sigue activo)."));
    }
}
