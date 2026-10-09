package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ClienteRegistroDTO;

public interface OnboardingService {
    String registrarCliente(ClienteRegistroDTO request);
}
