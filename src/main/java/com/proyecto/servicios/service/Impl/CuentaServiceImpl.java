package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.exception.DatoDuplicadoException;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.exception.RecursoInactivoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.request.cuenta.CuentaIdentificadorDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> listarTodas() {
        List<Cuenta> cuentas = cuentaRepository.findAll();
        if (cuentas.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay cuentas registradas en el sistema.");
        }
        return cuentas.stream().map(CuentaResponseDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CuentaResponseDTO> listarPaginado(Pageable pageable) {
        return cuentaRepository.findAll(pageable).map(CuentaResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDTO buscarPorNumero(CuentaIdentificadorDTO idDto, Long clienteId) {
        if (idDto == null || idDto.getNumeroCuenta() == null) {
            throw new RecursoNoEncontradoException("Se requiere el numero de cuenta para realizar la consulta.");
        }
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(idDto.getNumeroCuenta())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con numero: " + idDto.getNumeroCuenta()));

        if (!cuenta.getCliente().getId().equals(clienteId)) {
            throw new OperacionNoPermitidaException("Acceso denegado: esta cuenta pertenece a otro usuario.");
        }
        if (!cuenta.getActivo()) {
            throw new RecursoInactivoException("La cuenta estÃ¡ desactivada.");
        }

        return CuentaResponseDTO.fromEntity(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponseDTO crearCuenta(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + clienteId));

        if (!cliente.getActivo()) {
            throw new OperacionNoPermitidaException("El cliente esta inactivo. No puede crear cuentas.");
        }

        Cuenta nuevaCuenta = new Cuenta();
        nuevaCuenta.setCliente(cliente);
        nuevaCuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        nuevaCuenta.setSaldo(new BigDecimal("0.00"));
        nuevaCuenta.setActivo(true);
        nuevaCuenta.setCreatedAt(LocalDateTime.now());
        nuevaCuenta.setUpdatedAt(LocalDateTime.now());

        return CuentaResponseDTO.fromEntity(cuentaRepository.save(nuevaCuenta));
    }

    @Override
    @Transactional
    public void darDeBaja(CuentaIdentificadorDTO idDto, Long clienteId) {
        if (idDto == null || idDto.getNumeroCuenta() == null) {
            throw new RecursoNoEncontradoException("Se requiere el numero de cuenta para realizar la baja.");
        }
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(idDto.getNumeroCuenta())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con numero: " + idDto.getNumeroCuenta()));

        if (!cuenta.getCliente().getId().equals(clienteId)) {
            throw new OperacionNoPermitidaException("Acceso denegado: no tiene permisos sobre esta cuenta.");
        }
        if (!cuenta.getActivo()) {
            throw new DatoDuplicadoException("La cuenta ya estaba dada de baja.");
        }

        cuenta.setActivo(false);
        cuenta.setUpdatedAt(LocalDateTime.now());
        cuentaRepository.save(cuenta);
    }

    private String generarNumeroCuentaUnico() {
        Random random = new Random();
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