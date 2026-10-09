package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClienteRegistroDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "El nombre solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "El segundo nombre solo puede contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "El apellido paterno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "El apellido materno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El CURP es obligatorio")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z\\d]\\d$", message = "El formato de CURP es invalido")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Z&Ñ]{3,4}\\d{6}[A-Z\\d]{3}$", message = "El formato de RFC es invalido")
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
    @Size(max = 100, message = "El correo electronico no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono alternativo debe contener exactamente 10 digitos")
    private String telefonoAlternativo;

    @NotBlank(message = "La ocupacion es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @Positive(message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
            message = "La contraseña debe tener minimo 8 caracteres, 1 mayuscula, 1 minuscula, 1 numero y 1 caracter especial")
    private String password;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;
}
