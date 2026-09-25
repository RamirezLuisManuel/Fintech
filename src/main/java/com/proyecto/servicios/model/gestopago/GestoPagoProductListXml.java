package com.proyecto.servicios.model.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import lombok.Data;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement(name = "root")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProductListXml {

    @XmlElement(name = "producto")
    private List<GestoPagoProduct> productos;

}
