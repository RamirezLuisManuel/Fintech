package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import feign.Response;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class GestoPagoGatewayService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GestoPagoServiceClient client;
    private final GestoPagoEncryptionService encryptionService;
    private final GestoPagoTxXmlParser xmlParser;
    private final GestoPagoTokenService tokenService;

    public GestoPagoTxResponse sendTx(String payloadJson) {
        return execute(payloadJson, Operation.SEND);
    }

    public GestoPagoTxResponse confirmTx(String payloadJson) {
        return execute(payloadJson, Operation.CONFIRM);
    }

    public GestoPagoTxResponse verifyReference(String payloadJson) {
        return execute(payloadJson, Operation.VERIFY_REFERENCE);
    }

    private GestoPagoTxResponse execute(String payloadJson, Operation operation) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("random", System.currentTimeMillis() + "_" +
                RandomStringUtils.random(6, 0, 0, true, true, null, SECURE_RANDOM));
        form.add("signed", encryptionService.encrypt(payloadJson));

        Response response;
        try {
            response = switch (operation) {
                case SEND -> client.sendTx(form);
                case CONFIRM -> client.confirmTx(form);
                case VERIFY_REFERENCE -> client.verifyReference(form);
            };
        } catch (FeignException ex) {
            if (ex.status() == 401 || ex.status() == 403) {
                tokenService.invalidarTokenActual();
            }
            throw ex;
        }

        try (response) {
            if (response.body() == null) {
                throw new IllegalStateException("GestoPago devolvio una respuesta sin contenido");
            }
            String body = IOUtils.toString(response.body().asInputStream(), StandardCharsets.UTF_8);
            return xmlParser.parse(body);
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible leer la respuesta de GestoPago", ex);
        }
    }

    private enum Operation {
        SEND,
        CONFIRM,
        VERIFY_REFERENCE
    }
}
