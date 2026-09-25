package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@Entity
@Table(name = "gestopago_products")
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProduct {
    
    @Id
    @Column(name = "id_producto")
    @XmlAttribute
    private Integer idProducto;
    
    @XmlAttribute
    private String servicio;
    
    @XmlAttribute
    private String producto;
    
    @Column(name = "id_servicio")
    @XmlAttribute
    private Integer idServicio;
    
    @Column(name = "id_cat_tipo_servicio")
    @XmlAttribute
    private Integer idCatTipoServicio;
    
    @Column(name = "tipo_front")
    @XmlAttribute
    private Integer tipoFront;
    
    @Column(name = "has_digito_verificador")
    @XmlAttribute
    private Boolean hasDigitoVerificador;
    
    @XmlAttribute
    private Double precio;
    
    @Column(name = "show_ayuda")
    @XmlAttribute
    private Boolean showAyuda;
    
    @Column(name = "tipo_referencia")
    @XmlAttribute
    private String tipoReferencia;
    
    @Column(columnDefinition = "TEXT")
    @XmlElement
    private String legend;
}
