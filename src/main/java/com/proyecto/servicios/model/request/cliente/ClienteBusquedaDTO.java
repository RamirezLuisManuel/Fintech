package com.proyecto.servicios.model.request.cliente;

import lombok.Data;

/**
 * Request DTO para busqueda combinada en un unico endpoint con @RequestBody.
 * Elimina el uso de @RequestParam y @PathVariable.
 */
@Data
public class ClienteBusquedaDTO {
    private String criterio;
    private String rfc;
    private String curp;
    private String nombre;
    private String apellidoPaterno;
    private String correo;
    private Boolean activo = true;
    private Integer pagina = 0;
    private Integer tamanoPagina = 10;
}