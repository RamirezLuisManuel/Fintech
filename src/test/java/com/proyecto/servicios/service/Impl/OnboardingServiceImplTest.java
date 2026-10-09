package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.exception.OnboardingException;
import com.proyecto.servicios.model.onboarding.ClienteRegistroDTO;
import com.proyecto.servicios.model.onboarding.DomicilioDTO;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Modulo Onboarding - OnboardingServiceImpl")
class OnboardingServiceImplTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private CuentaRepository cuentaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OnboardingServiceImpl onboardingService;

    private ClienteRegistroDTO requestValido;

    @BeforeEach
    void setUp() {
        requestValido = new ClienteRegistroDTO();
        requestValido.setNombre("Juan");
        requestValido.setSegundoNombre("Carlos");
        requestValido.setApellidoPaterno("Perez");
        requestValido.setApellidoMaterno("Lopez");
        requestValido.setCorreo("juan@test.com");
        requestValido.setPassword("Segura123#");
        requestValido.setRfc("PERJ901212ABC");
        requestValido.setCurp("PERJ901212HDFRRL01");
        requestValido.setSexo("M");
        requestValido.setNacionalidad("Mexicana");
        requestValido.setEstadoCivil("Soltero");
        requestValido.setTelefonoMovil("4151234567");
        requestValido.setTelefonoAlternativo("4159876543");
        requestValido.setOcupacion("Ingeniero");
        requestValido.setEmpresa("ACME");
        requestValido.setIngresoMensual(new BigDecimal("15000.00"));
        requestValido.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        DomicilioDTO dom = new DomicilioDTO();
        dom.setCalle("Hidalgo");
        dom.setNumeroExterior("10");
        dom.setColonia("Centro");
        dom.setMunicipio("Dolores Hidalgo");
        dom.setEstado("Guanajuato");
        dom.setCodigoPostal("37800");
        dom.setPais("Mexico");
        requestValido.setDomicilio(dom);
    }

    @Test
    @DisplayName("TC-ONB-01: Registro exitoso devuelve numero de cuenta")
    void registrarCliente_exitoso_devuelveNumeroCuenta() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        String numeroCuenta = onboardingService.registrarCliente(requestValido);
        assertNotNull(numeroCuenta);
        assertEquals(10, numeroCuenta.length());
        verify(clienteRepository).save(any(Cliente.class));
        verify(cuentaRepository).save(any(Cuenta.class));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("TC-ONB-02: Registro exitoso persiste los 3 repositorios")
    void registrarCliente_exitoso_llamaLos3Repositorios() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        onboardingService.registrarCliente(requestValido);
        verify(clienteRepository, times(1)).save(any(Cliente.class));
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("TC-ONB-03: Registro codifica la contrasena con BCrypt")
    void registrarCliente_exitoso_codificaPasswordConBcrypt() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        onboardingService.registrarCliente(requestValido);
        verify(passwordEncoder, times(1)).encode("Segura123#");
    }

    @Test
    @DisplayName("TC-ONB-04: Registro falla si cliente es menor de edad")
    void registrarCliente_menorDeEdad_lanzaExcepcion() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(17));
        OnboardingException ex = assertThrows(OnboardingException.class,
                () -> onboardingService.registrarCliente(requestValido));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("mayor de edad"));
    }

    @Test
    @DisplayName("TC-ONB-05: Registro falla si edad es 17 anios y 364 dias")
    void registrarCliente_casiMayorDeEdad_lanzaExcepcion() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(18).plusDays(1));
        assertThrows(OnboardingException.class, () -> onboardingService.registrarCliente(requestValido));
    }

    @Test
    @DisplayName("TC-ONB-06: Registro exitoso con exactamente 18 anios")
    void registrarCliente_exactamente18Anios_exitoso() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(18));
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        assertDoesNotThrow(() -> onboardingService.registrarCliente(requestValido));
    }

    @Test
    @DisplayName("TC-ONB-07: Registro falla si CURP ya existe")
    void registrarCliente_curpDuplicado_lanzaConflicto() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);
        OnboardingException ex = assertThrows(OnboardingException.class,
                () -> onboardingService.registrarCliente(requestValido));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("CURP"));
    }

    @Test
    @DisplayName("TC-ONB-08: Registro falla si RFC ya existe")
    void registrarCliente_rfcDuplicado_lanzaConflicto() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);
        OnboardingException ex = assertThrows(OnboardingException.class,
                () -> onboardingService.registrarCliente(requestValido));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("RFC"));
    }

    @Test
    @DisplayName("TC-ONB-09: Registro falla si correo ya existe")
    void registrarCliente_correoDuplicado_lanzaConflicto() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(true);
        OnboardingException ex = assertThrows(OnboardingException.class,
                () -> onboardingService.registrarCliente(requestValido));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("correo"));
    }

    @Test
    @DisplayName("TC-ONB-10: Genera nuevo numero de cuenta si hay colision")
    void registrarCliente_numeroCuentaColision_generaOtro() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(true).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        String numeroCuenta = onboardingService.registrarCliente(requestValido);
        assertNotNull(numeroCuenta);
        verify(cuentaRepository, atLeast(2)).existsByNumeroCuenta(anyString());
    }

    @Test
    @DisplayName("TC-ONB-11: Cuenta inicial creada con saldo 1000.00 y activa")
    void registrarCliente_cuentaCreada_conSaldoInicialYActiva() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> {
            Cuenta c = inv.getArgument(0);
            assertEquals(new BigDecimal("1000.00"), c.getSaldo());
            assertTrue(c.getActivo());
            return c;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        onboardingService.registrarCliente(requestValido);
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
    }

    @Test
    @DisplayName("TC-ONB-12: Usuario creado con rol CLIENTE y faceIdEnabled=false")
    void registrarCliente_usuarioCreado_conRolClienteYFaceIdFalse() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            assertEquals("CLIENTE", u.getRol());
            assertFalse(u.getFaceIdEnabled());
            return u;
        });
        onboardingService.registrarCliente(requestValido);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }
}