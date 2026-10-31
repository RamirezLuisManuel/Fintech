package com.proyecto.servicios.service;

import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.RegistroClienteResponseDTO;

public interface OnboardingService {
    RegistroClienteResponseDTO registrarCliente(ClienteRegistroDTO request);
}
