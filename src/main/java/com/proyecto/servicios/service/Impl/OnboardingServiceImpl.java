package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.*;
import com.proyecto.servicios.exception.OnboardingException;
import com.proyecto.servicios.model.onboarding.ClienteRegistroDTO;
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

    @Override
    @Transactional
    public String registrarCliente(ClienteRegistroDTO request) {
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
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());

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

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol("CLIENTE");
        usuario.setFaceIdEnabled(false);
        usuarioRepository.save(usuario);

        return cuenta.getNumeroCuenta();
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

