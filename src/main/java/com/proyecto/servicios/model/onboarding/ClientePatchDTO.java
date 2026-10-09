package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO para actualizaciones parciales (PATCH) de un cliente.
 * Solo se pueden modificar datos de contacto y datos laborales.
 * Los campos identitarios (CURP, RFC) son inmutables una vez registrados.
 */
@Data
public class ClientePatchDTO {

    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "El nombre solo puede contener letras y espacios")
    @Size(min = 2, max = 50)
    private String nombre;

    @Pattern(regexp = "^[a-zA-Z\\s]*$")
    private String segundoNombre;

    @Pattern(regexp = "^[a-zA-Z\\s]+$")
    @Size(min = 2, max = 50)
    private String apellidoPaterno;

    @Pattern(regexp = "^[a-zA-Z\\s]+$")
    @Size(min = 2, max = 50)
    private String apellidoMaterno;

    @Email(message = "El formato del correo electronico es invalido")
    @Size(max = 100)
    private String correo;

    @Pattern(regexp = "^[0-9]{10}$", message = "El telefono movil debe tener exactamente 10 digitos")
    private String telefonoMovil;

    @Pattern(regexp = "^[0-9]{10}$")
    private String telefonoAlternativo;

    @Size(max = 20)
    private String estadoCivil;

    @Size(max = 100)
    private String ocupacion;

    @Size(max = 100)
    private String empresa;

    @DecimalMin(value = "0.0", message = "El ingreso mensual no puede ser negativo")
    private BigDecimal ingresoMensual;
}
