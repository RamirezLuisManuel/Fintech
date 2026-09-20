package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gestopago_products")
@Getter
@Setter
@NoArgsConstructor
public class GestoPagoProduct {
    
    @Id
    @Column(name = "id_producto")
    private Integer idProducto;
    
    private String servicio;
    
    private String producto;
    
    @Column(name = "id_servicio")
    private Integer idServicio;
    
    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;
    
    @Column(name = "tipo_front")
    private Integer tipoFront;
    
    @Column(name = "has_digito_verificador")
    private Boolean hasDigitoVerificador;
    
    private Double precio;
    
    @Column(name = "show_ayuda")
    private Boolean showAyuda;
    
    @Column(name = "tipo_referencia")
    private String tipoReferencia;
    
    @Column(columnDefinition = "TEXT")
    private String legend;
}
