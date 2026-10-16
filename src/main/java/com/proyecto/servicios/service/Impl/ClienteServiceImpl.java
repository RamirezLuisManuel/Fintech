package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.exception.DatoDuplicadoException;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.exception.RecursoInactivoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.request.cliente.ClienteBusquedaDTO;
import com.proyecto.servicios.model.request.cliente.ClienteGestionDTO;
import com.proyecto.servicios.model.request.cliente.ClienteIdentificadorDTO;
import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.ClienteResponseDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.specification.ClienteSpecification;
import com.proyecto.servicios.util.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscar(ClienteBusquedaDTO filtros) {
        int page = (filtros.getPagina() != null && filtros.getPagina() >= 0) ? filtros.getPagina() : 0;
        int size = (filtros.getTamanoPagina() != null && filtros.getTamanoPagina() > 0) ? filtros.getTamanoPagina() : 10;
        Pageable pageable = PageRequest.of(page, size);

        Specification<Cliente> spec = ClienteSpecification.conFiltros(filtros);
        return clienteRepository.findAll(spec, pageable).map(ClienteResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorIdentificador(ClienteIdentificadorDTO idDto) {
        Cliente cliente = resolverCliente(idDto);
        if (!cliente.getActivo()) {
            throw new RecursoInactivoException("El cliente ha sido dado de baja.");
        }
        return ClienteResponseDTO.fromEntity(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> obtenerCuentas(ClienteIdentificadorDTO idDto, Long clienteIdAutenticado) {
        Cliente cliente = resolverCliente(idDto);
        if (clienteIdAutenticado != null && !cliente.getId().equals(clienteIdAutenticado)) {
            throw new OperacionNoPermitidaException("Acceso denegado: no puede consultar cuentas de otro cliente.");
        }

        List<Cuenta> cuentas = cuentaRepository.findByClienteId(cliente.getId());
        if (cuentas.isEmpty()) {
            throw new RecursoNoEncontradoException("El cliente no tiene cuentas registradas.");
        }
        return cuentas.stream().map(CuentaResponseDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRegistroDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + id));

        if (!cliente.getActivo()) {
            throw new RecursoInactivoException("No se puede actualizar un cliente dado de baja.");
        }

        if (!cliente.getCorreo().equals(request.getCorreo()) &&
                clienteRepository.existsByCorreo(request.getCorreo())) {
            throw new DatoDuplicadoException("El correo electronico ya estÃ¡ en uso.");
        }

        BeanUtils.copyProperties(request, cliente, "id", "curp", "rfc", "activo", "createdAt", "domicilio", "usuario", "cuentas");
        cliente.setUpdatedAt(LocalDateTime.now());
        return ClienteResponseDTO.fromEntity(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponseDTO procesarGestionCliente(ClienteGestionDTO request) {
        Optional<Cliente> clienteOpt = Optional.empty();

        if (request.getRfc() != null && !request.getRfc().trim().isEmpty()) {
            clienteOpt = clienteRepository.findByRfc(request.getRfc().trim().toUpperCase());
        } else if (request.getCurp() != null && !request.getCurp().trim().isEmpty()) {
            clienteOpt = clienteRepository.findByCurp(request.getCurp().trim().toUpperCase());
        } else if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
            Page<Cliente> coincidencias = clienteRepository.findByNombreContainingIgnoreCase(request.getNombre().trim(), PageRequest.of(0, 1));
            if (!coincidencias.isEmpty()) {
                clienteOpt = Optional.of(coincidencias.getContent().get(0));
            }
        }

        if (clienteOpt.isEmpty()) {
            throw new RecursoNoEncontradoException("No se encontro ningun cliente con los datos proporcionados (RFC/CURP/Nombre).");
        }

        Cliente cliente = clienteOpt.get();

        if (Boolean.TRUE.equals(request.getDesactivar())) {
            if (!cliente.getActivo()) {
                throw new DatoDuplicadoException("El cliente ya estaba dado de baja.");
            }
            cliente.setActivo(false);
            cliente.setUpdatedAt(LocalDateTime.now());

            List<Cuenta> cuentas = cuentaRepository.findByClienteId(cliente.getId());
            for (Cuenta cuenta : cuentas) {
                cuenta.setActivo(false);
                cuenta.setUpdatedAt(LocalDateTime.now());
                cuentaRepository.save(cuenta);
            }

            return ClienteResponseDTO.fromEntity(clienteRepository.save(cliente));
        } else {
            if (!cliente.getActivo()) {
                throw new RecursoInactivoException("No se puede actualizar un cliente dado de baja.");
            }

            if (request.getCorreo() != null && !request.getCorreo().equals(cliente.getCorreo())) {
                if (clienteRepository.existsByCorreo(request.getCorreo())) {
                    throw new DatoDuplicadoException("El correo electronico ya esta en uso.");
                }
            }

            BeanCopyUtils.copyNonNullProperties(request, cliente, "id", "curp", "rfc", "activo", "createdAt", "updatedAt", "desactivar");
            cliente.setUpdatedAt(LocalDateTime.now());
            return ClienteResponseDTO.fromEntity(clienteRepository.save(cliente));
        }
    }

    @Override
    @Transactional
    public void darDeBaja(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + id));

        if (!cliente.getActivo()) {
            throw new DatoDuplicadoException("El cliente ya estaba dado de baja.");
        }

        cliente.setActivo(false);
        cliente.setUpdatedAt(LocalDateTime.now());

        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        for (Cuenta cuenta : cuentas) {
            cuenta.setActivo(false);
            cuenta.setUpdatedAt(LocalDateTime.now());
            cuentaRepository.save(cuenta);
        }

        clienteRepository.save(cliente);
    }

    private Cliente resolverCliente(ClienteIdentificadorDTO idDto) {
        if (idDto == null) {
            throw new RecursoNoEncontradoException("Se requiere un identificador (ID, RFC o CURP) para consultar el cliente.");
        }
        if (idDto.getId() != null) {
            return clienteRepository.findById(idDto.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + idDto.getId()));
        }
        if (idDto.getRfc() != null && !idDto.getRfc().trim().isEmpty()) {
            return clienteRepository.findByRfc(idDto.getRfc().trim().toUpperCase())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con RFC: " + idDto.getRfc()));
        }
        if (idDto.getCurp() != null && !idDto.getCurp().trim().isEmpty()) {
            return clienteRepository.findByCurp(idDto.getCurp().trim().toUpperCase())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con CURP: " + idDto.getCurp()));
        }
        throw new RecursoNoEncontradoException("Debe proporcionar al menos un ID, RFC o CURP valido.");
    }
}