package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.*;
import com.proyecto.servicios.exception.OnboardingException;
import com.proyecto.servicios.model.request.cliente.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.RegistroClienteResponseDTO;
import com.proyecto.servicios.repositorys.*;
import com.proyecto.servicios.service.OnboardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.util.Base64;
import java.util.Random;

@Service
public class OnboardingServiceImpl implements OnboardingService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private com.proyecto.servicios.service.TotpService totpService;

    @Override
    @Transactional
    public RegistroClienteResponseDTO registrarCliente(ClienteRegistroDTO request) {
        if (Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new OnboardingException(HttpStatus.BAD_REQUEST, "El cliente debe ser mayor de edad.");
        }

        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new OnboardingException(HttpStatus.CONFLICT, "La CURP ya esta registrada.");
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new OnboardingException(HttpStatus.CONFLICT, "El RFC ya esta registrado.");
        }
        if (clienteRepository.existsByCorreo(request.getCorreo())) {
            throw new OnboardingException(HttpStatus.CONFLICT, "El correo electronico ya esta registrado.");
        }

        boolean tieneEmpresa = request.getEmpresa() != null && !request.getEmpresa().isBlank();
        boolean tieneOcupacion = request.getOcupacion() != null && !request.getOcupacion().isBlank();
        boolean esEstudiante = tieneOcupacion && "Estudiante".equalsIgnoreCase(request.getOcupacion().trim());
        BigDecimal ingreso = request.getIngresoMensual() != null ? request.getIngresoMensual() : BigDecimal.ZERO;
        boolean tieneIngresoPositivo = ingreso.compareTo(BigDecimal.ZERO) > 0;

        if (tieneEmpresa && !tieneOcupacion) {
            throw new OnboardingException(HttpStatus.BAD_REQUEST, "Si ingresa una empresa, primero debe seleccionar una ocupacion.");
        }
        if (tieneOcupacion && !esEstudiante) {
            if (!tieneEmpresa) {
                throw new OnboardingException(HttpStatus.BAD_REQUEST, "Para la ocupacion seleccionada es obligatorio ingresar una empresa.");
            }
            if (!tieneIngresoPositivo) {
                throw new OnboardingException(HttpStatus.BAD_REQUEST, "Para la ocupacion seleccionada el ingreso mensual es obligatorio y debe ser mayor a cero.");
            }
        }
        if (tieneIngresoPositivo && !tieneOcupacion) {
            throw new OnboardingException(HttpStatus.BAD_REQUEST, "Para registrar un ingreso mensual es requisito seleccionar primero una ocupacion.");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreo(request.getCorreo());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setEmpresa(tieneEmpresa ? request.getEmpresa().trim() : null);
        cliente.setOcupacion(tieneOcupacion ? request.getOcupacion().trim() : null);
        cliente.setIngresoMensual(ingreso);

        Domicilio dom = new Domicilio();
        dom.setCalle(request.getDomicilio().getCalle());
        dom.setNumeroExterior(request.getDomicilio().getNumeroExterior());
        dom.setNumeroInterior(request.getDomicilio().getNumeroInterior());
        dom.setColonia(request.getDomicilio().getColonia());
        dom.setMunicipio(request.getDomicilio().getMunicipio());
        dom.setEstado(request.getDomicilio().getEstado());
        dom.setCodigoPostal(request.getDomicilio().getCodigoPostal());
        dom.setPais(request.getDomicilio().getPais());
        dom.setCliente(cliente);
        cliente.setDomicilio(dom);

        clienteRepository.save(cliente);

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(new BigDecimal("1000.00")); 
        cuenta.setActivo(true);
        cuentaRepository.save(cuenta);

        String pin = generarPinSeguro();
        String totpSecret = totpService.generateSecret();
        String totpUri = totpService.generateOtpAuthUri(totpSecret, cliente.getCorreo(), "FintechGestoPago");

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setPin(passwordEncoder.encode(pin));
        usuario.setTotpSecret(totpSecret);
        usuario.setRol("CLIENTE");
        usuario.setFaceIdEnabled(false);
        usuarioRepository.save(usuario);

        return RegistroClienteResponseDTO.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .pin(pin)
                .totpSecret(totpSecret)
                .totpUri(totpUri)
                .aviso("Guarde su PIN y configure Google Authenticator con su clave secreta.")
                .build();
    }

    private String generarPinSeguro() {
        SecureRandom random = new SecureRandom();
        return String.format("%04d", random.nextInt(10000));
    }

    private String generarNumeroCuentaUnico() {
        Random random = new Random();
        String cuenta;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            cuenta = sb.toString();
        } while (cuentaRepository.existsByNumeroCuenta(cuenta));
        return cuenta;
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
