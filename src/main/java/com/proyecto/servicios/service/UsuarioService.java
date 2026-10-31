package com.proyecto.servicios.service;

import com.proyecto.servicios.model.request.auth.AuthRequest;
import com.proyecto.servicios.model.response.AuthResponse;
import com.proyecto.servicios.model.request.auth.PasswordChangeDTO;
import com.proyecto.servicios.model.request.auth.PinChangeDTO;
import com.proyecto.servicios.model.request.auth.RegisterRequest;
import com.proyecto.servicios.model.request.auth.UsuarioIdentificadorDTO;
import com.proyecto.servicios.model.response.UsuarioResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UsuarioService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    List<UsuarioResponseDTO> listarTodos();
    Page<UsuarioResponseDTO> listarPaginado(Pageable pageable);
    void cambiarPassword(String username, PasswordChangeDTO request);
    void actualizarPin(String username, PinChangeDTO request);
    void bajaLogica(UsuarioIdentificadorDTO idDto);
}