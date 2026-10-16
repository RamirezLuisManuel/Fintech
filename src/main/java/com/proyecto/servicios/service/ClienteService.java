package com.proyecto.servicios.service;

import com.proyecto.servicios.model.request.cliente.ClienteBusquedaDTO;
import com.proyecto.servicios.model.request.cliente.ClienteGestionDTO;
import com.proyecto.servicios.model.request.cliente.ClienteIdentificadorDTO;
import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.ClienteResponseDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ClienteService {
    Page<ClienteResponseDTO> buscar(ClienteBusquedaDTO filtros);
    ClienteResponseDTO buscarPorIdentificador(ClienteIdentificadorDTO idDto);
    List<CuentaResponseDTO> obtenerCuentas(ClienteIdentificadorDTO idDto, Long clienteIdAutenticado);
    ClienteResponseDTO actualizar(Long id, ClienteRegistroDTO request);
    ClienteResponseDTO procesarGestionCliente(ClienteGestionDTO request);
    void darDeBaja(Long id);
}