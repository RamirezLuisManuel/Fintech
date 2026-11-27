package com.proyecto.servicios.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Servicio para generación y validación de códigos TOTP (Time-based One-Time Password)
 * compatible con Google Authenticator según la norma RFC 6238 y RFC 4226.
 */
@Service
public class TotpService {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTE_LENGTH = 20; // 160 bits recomendados
    private static final int TIME_STEP_SECONDS = 30;
    private static final int WINDOW_SIZE = 1; // Permite t-1, t, t+1 (ventana de +-30s por desfase horario)
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Genera un secreto aleatorio codificado en Base32 para el usuario.
     */
    public String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    /**
     * Genera la URI otpauth estándar para escaneo mediante código QR o importación manual
     * en Google Authenticator.
     */
    public String generateOtpAuthUri(String secret, String accountName, String issuer) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encodedIssuer, encodedAccount, secret, encodedIssuer);
    }

    /**
     * Valida si el código de 6 dígitos ingresado coincide con el algoritmo TOTP.
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("^\\d{6}$")) {
            return false;
        }

        byte[] keyBytes = decodeBase32(secret.trim().toUpperCase());
        if (keyBytes.length == 0) {
            return false;
        }

        long currentUnixTime = System.currentTimeMillis() / 1000L;
        long currentInterval = currentUnixTime / TIME_STEP_SECONDS;

        for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
            long testInterval = currentInterval + i;
            String generated = generateTotp(keyBytes, testInterval);
            if (code.equals(generated)) {
                return true;
            }
        }
        return false;
    }

    private String generateTotp(byte[] key, long interval) {
        try {
            byte[] data = new byte[8];
            long value = interval;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            return "";
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int next = 0;
        int bitsLeft = 0;

        while (next < data.length) {
            buffer <<= 8;
            buffer |= (data[next++] & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }

        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            sb.append(BASE32_CHARS.charAt(buffer & 0x1F));
        }

        return sb.toString();
    }

    private byte[] decodeBase32(String base32) {
        String clean = base32.replaceAll("[^A-Z2-7]", "");
        byte[] bytes = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (char c : clean.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer <<= 5;
            buffer |= val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                if (count < bytes.length) {
                    bytes[count++] = (byte) ((buffer >> bitsLeft) & 0xFF);
                }
            }
        }

        return Arrays.copyOf(bytes, count);
    }
}
