package com.proyecto.servicios.model.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinChangeDTO {

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "\\d{4,6}", message = "El PIN debe contener entre 4 y 6 digitos numericos")
    private String pin;
}