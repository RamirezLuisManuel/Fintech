package com.proyecto.servicios.model.request.cliente;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para gestion de cliente sin PathVariable.
 * Permite buscar al cliente por su identificador natural (RFC, CURP o Nombre)
 * y ejecutar o bien desactivacion (si la bandera desactivar=true)
 * o bien actualizacion de datos (si desactivar=false o null).
 */
@Data
public class ClienteGestionDTO {

    // Identificadores de busqueda sin PathVariable
    private String rfc;
    private String curp;

    // Bandera booleana para definir la accion
    // true -> Desactivacion (baja logica)
    // false/null -> Actualizacion de datos (PATCH)
    private Boolean desactivar = false;

    // Campos de actualizacion (opcionales)
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "El nombre solo puede contener letras y espacios")
    @Size(min = 2, max = 50)
    private String nombre;

    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;

    @Email(message = "El formato del correo electronico es invalido")
    @Size(max = 100)
    private String correo;

    @Pattern(regexp = "^[0-9]{10}$", message = "El telefono movil debe tener exactamente 10 digitos")
    private String telefonoMovil;

    private String telefonoAlternativo;
    private String estadoCivil;
    private String ocupacion;
    private String empresa;

    @DecimalMin(value = "0.0", message = "El ingreso mensual no puede ser negativo")
    private BigDecimal ingresoMensual;
}