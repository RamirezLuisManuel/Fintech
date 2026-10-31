package com.proyecto.servicios.client.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

@Slf4j
public class GestoPagoFeignConfig {

    private final GestoPagoTokenService tokenService;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.api-key:}")
    private String apiKey;

    public GestoPagoFeignConfig(GestoPagoTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // La autenticacion usa un cliente Feign separado; este interceptor protege las operaciones.
                if (!template.url().contains("/authenticate")) {
                    log.debug("Inyectando Bearer token en la peticion hacia Gestopago");
                    Optional<GestoPagoToken> tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
                    
                    if (tokenOpt.isPresent() && tokenOpt.get().getToken() != null) {
                        template.header("Authorization", "Bearer " + tokenOpt.get().getToken());
                        if (apiKey != null && !apiKey.isBlank()) {
                            template.header("X-API-Key", apiKey);
                        }
                    } else {
                        log.warn("No se encontro un token activo para idDistribuidor={} y codigoDispositivo={}", idDistribuidor, codigoDispositivo);
                        throw new IllegalStateException("No existe un token vigente para consumir GestoPago");
                    }
                }
            }
        };
    }
}
