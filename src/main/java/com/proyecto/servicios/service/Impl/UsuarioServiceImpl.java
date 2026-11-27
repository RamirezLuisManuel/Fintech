package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.exception.DatoDuplicadoException;
import com.proyecto.servicios.exception.OperacionNoPermitidaException;
import com.proyecto.servicios.exception.RecursoInactivoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.request.auth.AuthRequest;
import com.proyecto.servicios.model.response.AuthResponse;
import com.proyecto.servicios.model.response.CuentaResponseDTO;
import com.proyecto.servicios.model.request.auth.PasswordChangeDTO;
import com.proyecto.servicios.model.request.auth.PinChangeDTO;
import com.proyecto.servicios.model.request.auth.RegisterRequest;
import com.proyecto.servicios.model.request.auth.UsuarioIdentificadorDTO;
import com.proyecto.servicios.model.response.UsuarioResponseDTO;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.service.JwtService;
import com.proyecto.servicios.service.PaymentEmailService;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    @Autowired
    private com.proyecto.servicios.service.TotpService totpService;

    @Autowired(required = false)
    private PaymentEmailService paymentEmailService;

    private static final String REDIS_SESSION_PREFIX = "user_session:";
    private static final String REDIS_2FA_PREFIX = "user_2fa_otp:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public AuthResponse register(RegisterRequest request) {
        return new AuthResponse("dummy_access", "dummy_refresh", "12345");
    }

    @Override
    @Transactional
    public AuthResponse login(AuthRequest request) {
        Usuario usuario = usuarioRepository.findByClienteCorreo(request.getCorreo()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new RecursoInactivoException("El usuario ha sido dado de baja");
        }

        String correo = usuario.getCliente().getCorreo();
        redisTemplate.delete(REDIS_SESSION_PREFIX + correo);

        String newAccessToken = jwtService.generateToken(correo);
        String newRefreshToken = jwtService.generateRefreshToken(correo);

        usuario.setRefreshToken(newRefreshToken);
        usuarioRepository.save(usuario);

        redisTemplate.opsForValue().set(
                REDIS_SESSION_PREFIX + correo,
                newRefreshToken,
                java.time.Duration.ofMillis(jwtService.getJwtExpirationMs())
        );

        String numeroCuenta = "dummy_cuenta";
        Long clienteId = null;
        String nombreCompleto = correo;
        Cliente cliente = usuario.getCliente();
        if (cliente != null) {
            clienteId = cliente.getId();
            nombreCompleto = buildFullName(cliente, correo);
            if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
                numeroCuenta = cliente.getCuentas().stream()
                        .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                        .map(Cuenta::getNumeroCuenta)
                        .findFirst()
                        .orElse(cliente.getCuentas().get(0).getNumeroCuenta());
            }
        }

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .numeroCuenta(numeroCuenta)
                .clienteId(clienteId)
                .nombreCompleto(nombreCompleto)
                .correo(correo)
                .requires2fa(false)
                .activeSessionDetected(false)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse preLogin(AuthRequest request) {
        Usuario usuario = usuarioRepository.findByClienteCorreo(request.getCorreo()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new RecursoInactivoException("El usuario ha sido dado de baja");
        }

        String correo = usuario.getCliente().getCorreo();
        Object redisSession = null;
        try {
            redisSession = redisTemplate.opsForValue().get(REDIS_SESSION_PREFIX + correo);
        } catch (Exception ignored) {
        }

        // Solo se considera sesión activa si el token en Redis existe Y sigue vigente (no caducado)
        boolean redisActive = redisSession != null && jwtService.isTokenStillValid(redisSession.toString());
        if (!redisActive && (redisSession != null || (usuario.getRefreshToken() != null && !usuario.getRefreshToken().isBlank()))) {
            // Limpiar automáticamente sesiones caducadas para no mostrar falsos avisos de sesión activa
            try {
                redisTemplate.delete(REDIS_SESSION_PREFIX + correo);
            } catch (Exception ignored) {
            }
            usuario.setRefreshToken(null);
            usuarioRepository.save(usuario);
        }
        boolean hasActiveSession = redisActive;

        if (hasActiveSession && !Boolean.TRUE.equals(request.getForceClosePrevious())) {
            return AuthResponse.builder()
                    .correo(correo)
                    .activeSessionDetected(true)
                    .requires2fa(false)
                    .activeSessionInfo("Se detecto una sesion activa en otro dispositivo o navegador. Confirma si deseas finalizar la sesion anterior para continuar.")
                    .build();
        }

        if (Boolean.TRUE.equals(request.getForceClosePrevious())) {
            try {
                redisTemplate.delete(REDIS_SESSION_PREFIX + correo);
            } catch (Exception ignored) {
            }
            usuario.setRefreshToken(null);
            usuarioRepository.save(usuario);
        }

        if (usuario.getTotpSecret() == null || usuario.getTotpSecret().isBlank()) {
            usuario.setTotpSecret(totpService.generateSecret());
            usuarioRepository.save(usuario);
        }

        return AuthResponse.builder()
                .correo(correo)
                .activeSessionDetected(false)
                .requires2fa(true)
                .mensaje2fa("Ingresa tu código de 6 dígitos de Google Authenticator o tu PIN de 4 dígitos.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verify2faAndLogin(AuthRequest request) {
        Usuario usuario = usuarioRepository.findByClienteCorreo(request.getCorreo()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new RecursoInactivoException("El usuario ha sido dado de baja");
        }

        String code = request.getCodigo2fa() != null ? request.getCodigo2fa().trim() : "";
        if (code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de segundo factor (Google Authenticator / PIN) es obligatorio");
        }

        String correo = usuario.getCliente().getCorreo();

        boolean validTotp = usuario.getTotpSecret() != null && totpService.verifyCode(usuario.getTotpSecret(), code);
        boolean validPin = usuario.getPin() != null && passwordEncoder.matches(code, usuario.getPin());

        boolean validOtp = false;
        try {
            Object cachedOtp = redisTemplate.opsForValue().get(REDIS_2FA_PREFIX + correo);
            if (cachedOtp != null && code.equals(cachedOtp.toString())) {
                validOtp = true;
                redisTemplate.delete(REDIS_2FA_PREFIX + correo);
            }
        } catch (Exception ignored) {
        }

        if (!validTotp && !validPin && !validOtp) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de Google Authenticator (6 dígitos) o PIN de seguridad incorrecto");
        }

        return login(request);
    }

    @Override
    @Transactional
    public AuthResponse refreshSession(String correo, String refreshToken) {
        if (correo == null || correo.isBlank() || refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida.");
        }
        Usuario usuario = usuarioRepository.findByClienteCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
        Object redisSession = null;
        try {
            redisSession = redisTemplate.opsForValue().get(REDIS_SESSION_PREFIX + correo);
        } catch (Exception ignored) {
        }
        if (redisSession == null || !refreshToken.equals(redisSession.toString()) || !jwtService.isTokenStillValid(refreshToken)) {
            logout(correo);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESION_DESPLAZADA: La sesion ya no es valida.");
        }
        String newAccessToken = jwtService.generateToken(correo);
        try {
            redisTemplate.opsForValue().set(
                    REDIS_SESSION_PREFIX + correo,
                    refreshToken,
                    java.time.Duration.ofMillis(jwtService.getJwtExpirationMs())
            );
        } catch (Exception ignored) {
        }
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .correo(correo)
                .build();
    }

    @Override
    @Transactional
    public void logout(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(REDIS_SESSION_PREFIX + username);
            redisTemplate.delete(REDIS_2FA_PREFIX + username);
        } catch (Exception ignored) {
        }
        usuarioRepository.findByClienteCorreo(username).ifPresent(u -> {
            u.setRefreshToken(null);
            usuarioRepository.save(u);
        });
    }

    private String buildFullName(Cliente c, String fallback) {
        String full = java.util.stream.Stream.of(
                c.getNombre(),
                c.getSegundoNombre(),
                c.getApellidoPaterno(),
                c.getApellidoMaterno())
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return full.isEmpty() ? fallback : full;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPerfilActual(String username) {
        Usuario usuario = usuarioRepository.findByClienteCorreo(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + username));
        Cliente c = usuario.getCliente();
        List<CuentaResponseDTO> cuentas = c.getCuentas() != null
                ? c.getCuentas().stream().map(CuentaResponseDTO::fromEntity).collect(Collectors.toList())
                : List.of();

        String nombreCompleto = buildFullName(c, c.getCorreo());

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("usuarioId", usuario.getId());
        perfil.put("clienteId", c.getId());
        perfil.put("nombre", c.getNombre());
        perfil.put("segundoNombre", c.getSegundoNombre());
        perfil.put("apellidoPaterno", c.getApellidoPaterno());
        perfil.put("apellidoMaterno", c.getApellidoMaterno());
        perfil.put("nombreCompleto", nombreCompleto);
        perfil.put("correo", c.getCorreo());
        perfil.put("telefonoMovil", c.getTelefonoMovil());
        perfil.put("rfc", c.getRfc());
        perfil.put("curp", c.getCurp());
        perfil.put("cuentas", cuentas);
        return perfil;
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
