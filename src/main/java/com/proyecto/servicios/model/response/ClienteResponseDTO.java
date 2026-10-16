package com.proyecto.servicios.model.response;

import com.proyecto.servicios.entity.Cliente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClienteResponseDTO fromEntity(Cliente c) {
        if (c == null) return null;
        return ClienteResponseDTO.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .segundoNombre(c.getSegundoNombre())
                .apellidoPaterno(c.getApellidoPaterno())
                .apellidoMaterno(c.getApellidoMaterno())
                .fechaNacimiento(c.getFechaNacimiento())
                .curp(c.getCurp())
                .rfc(c.getRfc())
                .sexo(c.getSexo())
                .nacionalidad(c.getNacionalidad())
                .estadoCivil(c.getEstadoCivil())
                .correo(c.getCorreo())
                .telefonoMovil(c.getTelefonoMovil())
                .telefonoAlternativo(c.getTelefonoAlternativo())
                .ocupacion(c.getOcupacion())
                .empresa(c.getEmpresa())
                .ingresoMensual(c.getIngresoMensual())
                .activo(c.getActivo())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}