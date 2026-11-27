package com.proyecto.servicios.model.request.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DomicilioDTO {

    @NotBlank(message = "La calle es obligatoria")
    @Size(min = 3, max = 100, message = "La calle debe tener entre 3 y 100 caracteres")
    private String calle;

    @NotBlank(message = "El numero exterior es obligatorio")
    @Size(max = 20, message = "El numero exterior no puede exceder 20 caracteres")
    private String numeroExterior;

    @Size(max = 20, message = "El numero interior no puede exceder 20 caracteres")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no puede exceder 100 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no puede exceder 100 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no puede exceder 100 caracteres")
    private String estado;

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El codigo postal debe contener exactamente 5 digitos")
    private String codigoPostal;

    @NotBlank(message = "El pais es obligatorio")
    @Size(max = 100, message = "El pais no puede exceder 100 caracteres")
    private String pais;
}
