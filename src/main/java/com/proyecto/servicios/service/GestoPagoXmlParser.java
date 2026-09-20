package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GestoPagoXmlParser {

    public List<GestoPagoProduct> parseXml(String rawXml) {
        List<GestoPagoProduct> productos = new ArrayList<>();
        if (rawXml == null || rawXml.trim().isEmpty()) {
            return productos;
        }

        try {
            // Eliminar la declaracion <?xml ... ?> si existe porque no puede ir dentro del tag <root>
            String cleanXml = rawXml.replaceAll("(?i)<\\?xml[^>]*\\?>", "").trim();
            
            // Envolver en un root tag por si el XML viene como multiples nodos raiz sin envoltorio
            String wrappedXml = "<root>" + cleanXml + "</root>";
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Especificar explícitamente UTF-8 usando ByteArrayInputStream para mantener los caracteres especiales correctos
            Document document = builder.parse(new ByteArrayInputStream(wrappedXml.getBytes(StandardCharsets.UTF_8)));
            document.getDocumentElement().normalize();

            NodeList nodeList = document.getElementsByTagName("producto");
            
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    
                    GestoPagoProduct p = new GestoPagoProduct();
                    
                    p.setIdProducto(parseInteger(element.getAttribute("idProducto")));
                    p.setServicio(element.getAttribute("servicio"));
                    p.setProducto(element.getAttribute("producto"));
                    p.setIdServicio(parseInteger(element.getAttribute("idServicio")));
                    p.setIdCatTipoServicio(parseInteger(element.getAttribute("idCatTipoServicio")));
                    p.setTipoFront(parseInteger(element.getAttribute("tipoFront")));
                    p.setHasDigitoVerificador(parseBoolean(element.getAttribute("hasDigitoVerificador")));
                    p.setPrecio(parseDouble(element.getAttribute("precio")));
                    p.setShowAyuda(parseBoolean(element.getAttribute("showAyuda")));
                    p.setTipoReferencia(element.getAttribute("tipoReferencia"));
                    
                    // Obtener la leyenda que está dentro del CDATA
                    NodeList legendNodes = element.getElementsByTagName("legend");
                    if (legendNodes.getLength() > 0) {
                        p.setLegend(legendNodes.item(0).getTextContent());
                    }
                    
                    productos.add(p);
                }
            }
        } catch (Exception e) {
            log.error("Error al parsear el XML de GestoPago: {}", e.getMessage(), e);
        }
        
        return productos;
    }

    private Integer parseInteger(String val) {
        try {
            return (val != null && !val.isEmpty()) ? Integer.parseInt(val) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double parseDouble(String val) {
        try {
            return (val != null && !val.isEmpty()) ? Double.parseDouble(val) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Boolean parseBoolean(String val) {
        return (val != null && !val.isEmpty()) ? Boolean.parseBoolean(val) : null;
    }
}
