package com.proyecto.servicios.service;

import com.proyecto.servicios.model.AuthRequest;
import com.proyecto.servicios.model.AuthResponse;
import com.proyecto.servicios.model.RegisterRequest;

public interface UsuarioService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
}
