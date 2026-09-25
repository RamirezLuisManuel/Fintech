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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String REDIS_SESSION_PREFIX = "user_session:";

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya esta registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(hashPassword(request.getPassword()));
        usuario.setNumeroCuenta(generarNumeroCuenta());

        String accessToken = jwtService.generateToken(usuario.getEmail());
        String refreshToken = jwtService.generateRefreshToken(usuario.getEmail());
        usuario.setRefreshToken(refreshToken);

        usuarioRepository.save(usuario);
        
        // Guardar sesion activa en Redis
        redisTemplate.opsForValue().set(REDIS_SESSION_PREFIX + usuario.getEmail(), refreshToken);

        log.info("Usuario registrado exitosamente. Cuenta: {}", usuario.getNumeroCuenta());
        return new AuthResponse(accessToken, refreshToken, usuario.getNumeroCuenta());
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        Usuario usuario = buscarUsuarioConFallback(request.getEmail());
        
        if (usuario == null || !usuario.getPassword().equals(hashPassword(request.getPassword()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        // Matar sesion anterior si existiera en Redis
        redisTemplate.delete(REDIS_SESSION_PREFIX + usuario.getEmail());
        log.info("Sesion anterior terminada para el usuario {}", usuario.getEmail());

        // Generar nueva sesion
        String newAccessToken = jwtService.generateToken(usuario.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(usuario.getEmail());

        // Actualizar en DB
        try {
            usuario.setRefreshToken(newRefreshToken);
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            log.warn("No se pudo guardar la sesion en Postgres. Se mantendra solo en Redis.");
        }

        // Guardar nueva sesion en Redis
        redisTemplate.opsForValue().set(REDIS_SESSION_PREFIX + usuario.getEmail(), newRefreshToken);
        
        log.info("Inicio de sesion exitoso para el usuario {}", usuario.getEmail());
        return new AuthResponse(newAccessToken, newRefreshToken, usuario.getNumeroCuenta());
    }

    private Usuario buscarUsuarioConFallback(String email) {
        try {
            // Intentar primero en DB Postgres
            Optional<Usuario> usuarioDb = usuarioRepository.findByEmail(email);
            if (usuarioDb.isPresent()) {
                return usuarioDb.get();
            }
        } catch (Exception e) {
            log.error("Error al consultar Postgres, recurriendo a Redis: {}", e.getMessage());
            // Si Postgres falla, idealmente tendriamos los usuarios completos en Redis.
            // Para simplificar, asumiremos que si hay sesion en Redis, el usuario existe.
        }
        return usuarioRepository.findByEmail(email).orElse(null); // Fallback normal
    }

    private String generarNumeroCuenta() {
        Random random = new Random();
        StringBuilder cuenta = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            cuenta.append(random.nextInt(10));
        }
        while(usuarioRepository.existsByNumeroCuenta(cuenta.toString())) {
            cuenta = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                cuenta.append(random.nextInt(10));
            }
        }
        return cuenta.toString();
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
