package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse<T> {
    private Integer codigo;
    private String mensaje;
    private T data;

    public GenericResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public static <T> GenericResponse<T> exito(T data) {
        return new GenericResponse<>(0, "Operacion exitosa", data);
    }

    public static <T> GenericResponse<T> exito(String mensaje, T data) {
        return new GenericResponse<>(0, mensaje, data);
    }

    public static <T> GenericResponse<T> errorValidacion(String mensaje) {
        return new GenericResponse<>(1, mensaje, null);
    }

    public static <T> GenericResponse<T> errorNegocio(String mensaje) {
        return new GenericResponse<>(2, mensaje, null);
    }
}
