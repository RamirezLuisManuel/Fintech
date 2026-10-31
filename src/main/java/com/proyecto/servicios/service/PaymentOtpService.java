package com.proyecto.servicios.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentOtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String OTP_PREFIX = "gestopago:otp:";
    private static final String RATE_PREFIX = "gestopago:otp-rate:";

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final int expirationMinutes;
    private final int maxAttempts;
    private final int maxChallengesPerWindow;

    public PaymentOtpService(
            StringRedisTemplate redisTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${payments.otp.expiration-minutes:5}") int expirationMinutes,
            @Value("${payments.otp.max-attempts:5}") int maxAttempts,
            @Value("${payments.otp.max-challenges-per-10-minutes:5}") int maxChallengesPerWindow) {
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
        this.maxAttempts = maxAttempts;
        this.maxChallengesPerWindow = maxChallengesPerWindow;
    }

    public IssuedOtp issue(UUID operationId, Long userId) {
        enforceRateLimit(userId);
        String code = RandomStringUtils.random(6, 0, 0, false, true, null, SECURE_RANDOM);
        String key = OTP_PREFIX + operationId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                "hash", passwordEncoder.encode(code),
                "userId", userId.toString(),
                "attempts", Integer.toString(maxAttempts)
        ));
        redisTemplate.expire(key, Duration.ofMinutes(expirationMinutes));
        return new IssuedOtp(code, LocalDateTime.now().plusMinutes(expirationMinutes));
    }

    public void validateAndConsume(UUID operationId, Long userId, String code) {
        String key = OTP_PREFIX + operationId;
        Object storedUserId = redisTemplate.opsForHash().get(key, "userId");
        Object storedHash = redisTemplate.opsForHash().get(key, "hash");
        Object storedAttempts = redisTemplate.opsForHash().get(key, "attempts");

        if (storedUserId == null || storedHash == null || storedAttempts == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "El codigo de autenticacion expiro o ya fue utilizado");
        }
        if (!userId.toString().equals(storedUserId.toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El codigo no pertenece al usuario autenticado");
        }

        int attempts = Integer.parseInt(storedAttempts.toString());
        if (!passwordEncoder.matches(code, storedHash.toString())) {
            attempts--;
            if (attempts <= 0) {
                redisTemplate.delete(key);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Se agotaron los intentos del codigo de autenticacion");
            }
            redisTemplate.opsForHash().put(key, "attempts", Integer.toString(attempts));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Codigo de autenticacion incorrecto. Intentos restantes: " + attempts);
        }

        redisTemplate.delete(key);
    }

    public void delete(UUID operationId) {
        redisTemplate.delete(OTP_PREFIX + operationId);
    }

    private void enforceRateLimit(Long userId) {
        String key = RATE_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(10));
        }
        if (count != null && count > maxChallengesPerWindow) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Se alcanzo el limite de codigos de autenticacion. Intenta mas tarde");
        }
    }

    public record IssuedOtp(String code, LocalDateTime expiresAt) {}
}
