package com.proyecto.servicios.service;

import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;

public interface OnboardingService {
    String registrarCliente(ClienteRegistroDTO request);
}
