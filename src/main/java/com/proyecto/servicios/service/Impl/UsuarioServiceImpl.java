package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.exception.DatoDuplicadoException;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.exception.RecursoInactivoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.request.auth.AuthRequest;
import com.proyecto.servicios.model.response.AuthResponse;
import com.proyecto.servicios.model.request.auth.PasswordChangeDTO;
import com.proyecto.servicios.model.request.auth.PinChangeDTO;
import com.proyecto.servicios.model.request.auth.RegisterRequest;
import com.proyecto.servicios.model.request.auth.UsuarioIdentificadorDTO;
import com.proyecto.servicios.model.response.UsuarioResponseDTO;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.service.JwtService;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        return new AuthResponse("dummy_access", "dummy_refresh", "12345");
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        Usuario usuario = usuarioRepository.findByClienteCorreo(request.getCorreo()).orElse(null);
        
        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new RecursoInactivoException("El usuario ha sido dado de baja");
        }

        redisTemplate.delete(REDIS_SESSION_PREFIX + usuario.getCliente().getCorreo());
        
        String newAccessToken = jwtService.generateToken(usuario.getCliente().getCorreo());
        String newRefreshToken = jwtService.generateRefreshToken(usuario.getCliente().getCorreo());

        usuario.setRefreshToken(newRefreshToken);
        usuarioRepository.save(usuario);

        redisTemplate.opsForValue().set(REDIS_SESSION_PREFIX + usuario.getCliente().getCorreo(), newRefreshToken);
        
        return new AuthResponse(newAccessToken, newRefreshToken, "dummy_cuenta");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        if (usuarios.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay usuarios registrados en el sistema.");
        }
        return usuarios.stream().map(UsuarioResponseDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listarPaginado(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponseDTO::fromEntity);
    }

    @Override
    @Transactional
    public void cambiarPassword(String username, PasswordChangeDTO request) {
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseÃƒÂ±a y su confirmaciÃƒÂ³n no coinciden.");
        }

        Usuario usuario = usuarioRepository.findByClienteCorreo(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + username));

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseÃƒÂ±a actual es incorrecta.");
        }

        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPassword())) {
            throw new DatoDuplicadoException("La nueva contraseÃƒÂ±a no puede ser igual a la contraseÃƒÂ±a actual.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void bajaLogica(UsuarioIdentificadorDTO idDto) {
        if (idDto == null || idDto.getId() == null) {
            throw new RecursoNoEncontradoException("Se requiere el ID del usuario para la baja.");
        }
        Usuario usuario = usuarioRepository.findById(idDto.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + idDto.getId()));

        if (!usuario.getActivo()) {
            throw new DatoDuplicadoException("El usuario ya estaba dado de baja.");
        }

        usuario.setActivo(false);
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
    @Override
    @Transactional
    public void actualizarPin(String username, PinChangeDTO request) {
        Usuario usuario = usuarioRepository.findByClienteCorreo(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + username));
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new RecursoInactivoException("El usuario se encuentra inactivo");
        }
        usuario.setPin(passwordEncoder.encode(request.getPin()));
        usuarioRepository.save(usuario);
    }
}