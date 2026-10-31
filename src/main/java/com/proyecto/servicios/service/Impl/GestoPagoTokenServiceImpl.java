package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    @Autowired
    private GestoPagoAuthClient gestoPagoAuthClient;
    
    @Autowired
    private GestoPagoTokenRepository tokenRepository;
    
    @Autowired
    private GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}",
            initialDelayString = "${gestopago.auth.initial-delay-ms:10000}")
    public synchronized void renovarToken() {
        Optional<GestoPagoToken> current = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivoAndActivoTrue(idDistribuidor, codigoDispositivo);
        if (current.filter(this::isUsable).isPresent()) {
            return;
        }
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || response.getToken() == null) {
                log.error("La respuesta de GestoPago no contiene token");
                throw new RuntimeException("Respuesta de GestoPago Auth sin token");
            }

            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(idDistribuidor);
                        nuevo.setCodigoDispositivo(codigoDispositivo);
                        nuevo.setActivo(true);
                        return nuevo;
                    });

            long lifetimeSeconds = response.getExpiresIn() != null && response.getExpiresIn() > 0
                    ? response.getExpiresIn() : 86400L;
            tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(lifetimeSeconds));
            tokenEntity.setActivo(true);
            tokenRepository.save(tokenEntity);
            log.info("Token GestoPago renovado correctamente");

        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage(), e);
            log.warn("No se obtuvo un token nuevo de GestoPago");
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        Optional<GestoPagoToken> token = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivoAndActivoTrue(idDistribuidor, codigoDispositivo)
                .filter(this::isUsable);
        if (token.isEmpty()) {
            renovarToken();
            token = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivoAndActivoTrue(idDistribuidor, codigoDispositivo)
                    .filter(this::isUsable);
        }
        return token;
    }

    @Override
    public synchronized void invalidarTokenActual() {
        tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .ifPresent(token -> {
                    token.setActivo(false);
                    token.setExpiresAt(LocalDateTime.now());
                    tokenRepository.save(token);
                });
    }

    private boolean isUsable(GestoPagoToken token) {
        if (!Boolean.TRUE.equals(token.getActivo()) || token.getToken() == null) return false;
        LocalDateTime expiresAt = token.getExpiresAt();
        if (expiresAt == null && token.getExpiresIn() != null && token.getFechaActualizacion() != null) {
            expiresAt = token.getFechaActualizacion().plusSeconds(token.getExpiresIn());
        }
        return expiresAt != null && expiresAt.isAfter(LocalDateTime.now().plusMinutes(5));
    }
}
