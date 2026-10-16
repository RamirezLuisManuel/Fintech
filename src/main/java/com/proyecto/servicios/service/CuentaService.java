package com.proyecto.servicios.service;

import com.proyecto.servicios.model.request.cuenta.CuentaIdentificadorDTO;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CuentaService {
    List<CuentaResponseDTO> listarTodas();
    Page<CuentaResponseDTO> listarPaginado(Pageable pageable);
    CuentaResponseDTO buscarPorNumero(CuentaIdentificadorDTO idDto, Long clienteId);
    CuentaResponseDTO crearCuenta(Long clienteId);
    void darDeBaja(CuentaIdentificadorDTO idDto, Long clienteId);
}