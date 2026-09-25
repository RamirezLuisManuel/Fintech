package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoProductDTO {
    private Integer idProducto;
    private String servicio;
    private String producto;
    private Integer idServicio;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;
    private Double precio;
    private Boolean showAyuda;
    private String tipoReferencia;
    private String legend;
}
