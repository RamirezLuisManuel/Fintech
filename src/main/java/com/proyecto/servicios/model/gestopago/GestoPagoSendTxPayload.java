package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"idProducto", "idServicio", "telefono", "horaLocal", "referencia", "montoPago", "upc", "unidad"})
public class GestoPagoSendTxPayload {
    private Integer idProducto;
    private Integer idServicio;
    private String telefono;
    private String horaLocal;
    private String referencia;
    private BigDecimal montoPago;
    private String upc;
    private String unidad;
}
