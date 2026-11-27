package com.proyecto.servicios.model.request.cliente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClienteRegistroDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[A-ZÁÉÍÓÚÑ][a-zA-ZÁÉÍÓÚáéíóúÑñ]*$", message = "El nombre debe iniciar con mayuscula y no puede contener espacios ni numeros")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^([A-ZÁÉÍÓÚÑ][a-zA-ZÁÉÍÓÚáéíóúÑñ]*)?$", message = "El segundo nombre debe iniciar con mayuscula y no puede contener espacios")
    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[A-ZÁÉÍÓÚÑ][a-zA-ZÁÉÍÓÚáéíóúÑñ]*$", message = "El apellido paterno debe iniciar con mayuscula y no puede contener espacios")
    @Size(min = 3, max = 50, message = "El apellido paterno debe tener entre 3 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[A-ZÁÉÍÓÚÑ][a-zA-ZÁÉÍÓÚáéíóúÑñ]*$", message = "El apellido materno debe iniciar con mayuscula y no puede contener espacios")
    @Size(min = 3, max = 50, message = "El apellido materno debe tener entre 3 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El CURP es obligatorio")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "La CURP debe contener solo letras mayusculas y numeros sin signos")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Z]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El RFC debe contener solo letras mayusculas y numeros sin signos")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^[MF]$", message = "El sexo debe ser 'M' o 'F'")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    private String estadoCivil;

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "Formato de correo electronico invalido")
    @Pattern(regexp = "^(?i)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(com|com\\.mx|mx|org|net|edu|gob)$", message = "El correo debe terminar en un dominio valido como .com o .mx")
    @Size(max = 100, message = "El correo electronico no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono alternativo debe contener exactamente 10 digitos")
    private String telefonoAlternativo;

    @Size(max = 100, message = "La ocupacion no puede exceder 100 caracteres")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    private String empresa;

    @PositiveOrZero(message = "El ingreso mensual no puede ser negativo")
    private BigDecimal ingresoMensual;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 13, message = "La contraseña debe tener entre 8 y 13 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*._-]).{8,13}$",
            message = "La contraseña debe tener entre 8 y 13 caracteres, 1 mayuscula, 1 minuscula, 1 numero y 1 caracter especial")
    private String password;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;
}
