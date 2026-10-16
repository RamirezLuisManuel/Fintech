package com.proyecto.servicios.model.request.cliente;

import lombok.Data;

/**
 * DTO para consultar un cliente sin usar @PathVariable.
 */
@Data
public class ClienteIdentificadorDTO {
    private Long id;
    private String rfc;
    private String curp;
}