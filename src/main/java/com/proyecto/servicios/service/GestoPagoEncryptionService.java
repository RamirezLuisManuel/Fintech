package com.proyecto.servicios.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class GestoPagoEncryptionService {

    private final byte[] key;
    private final byte[] iv;

    public GestoPagoEncryptionService(
            @Value("${gestopago.encryption.secret-key}") String secretKey,
            @Value("${gestopago.encryption.iv}") String initializationVector) {
        this.key = Base64.getDecoder().decode(secretKey);
        this.iv = Base64.getDecoder().decode(initializationVector);
        if (key.length != 32) {
            throw new IllegalArgumentException("La llave AES de GestoPago debe contener 32 bytes al decodificar Base64");
        }
        if (iv.length != 16) {
            throw new IllegalArgumentException("El IV de GestoPago debe contener 16 bytes al decodificar Base64");
        }
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible cifrar el payload para GestoPago", ex);
        }
    }
}
