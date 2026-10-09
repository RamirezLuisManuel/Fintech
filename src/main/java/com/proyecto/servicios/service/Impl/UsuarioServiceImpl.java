package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.model.AuthRequest;
import com.proyecto.servicios.model.AuthResponse;
import com.proyecto.servicios.model.RegisterRequest;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.service.JwtService;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String REDIS_SESSION_PREFIX = "user_session:";

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Logica dummy temporal para que compile.
        // Se reemplazara en la Fase 4 con OnboardingService.
        return new AuthResponse("dummy_access", "dummy_refresh", "12345");
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        Usuario usuario = usuarioRepository.findByClienteCorreo(request.getCorreo()).orElse(null);
        
        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario ha sido dado de baja");
        }

        redisTemplate.delete(REDIS_SESSION_PREFIX + usuario.getCliente().getCorreo());
        
        String newAccessToken = jwtService.generateToken(usuario.getCliente().getCorreo());
        String newRefreshToken = jwtService.generateRefreshToken(usuario.getCliente().getCorreo());

        usuario.setRefreshToken(newRefreshToken);
        usuarioRepository.save(usuario);

        redisTemplate.opsForValue().set(REDIS_SESSION_PREFIX + usuario.getCliente().getCorreo(), newRefreshToken);
        
        return new AuthResponse(newAccessToken, newRefreshToken, "dummy_cuenta");
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear password", e);
        }
    }
}
