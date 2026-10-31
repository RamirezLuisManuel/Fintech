package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GestoPagoXmlParser {

    public List<GestoPagoProduct> parseXml(String rawXml) {
        if (rawXml == null || rawXml.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Deshabilitar DTD y entidades externas para prevenir vulnerabilidades XXE
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setNamespaceAware(false);

            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(rawXml)));

            NodeList nodeList = doc.getElementsByTagName("producto");
            List<GestoPagoProduct> products = new ArrayList<>(nodeList.getLength());

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    GestoPagoProduct p = new GestoPagoProduct();

                    if (el.hasAttribute("idProducto")) {
                        p.setIdProducto(parseIntSafe(el.getAttribute("idProducto")));
                    }
                    if (el.hasAttribute("idServicio")) {
                        p.setIdServicio(parseIntSafe(el.getAttribute("idServicio")));
                    }
                    if (el.hasAttribute("servicio")) {
                        p.setServicio(el.getAttribute("servicio"));
                    }
                    if (el.hasAttribute("producto")) {
                        p.setProducto(el.getAttribute("producto"));
                    }
                    if (el.hasAttribute("idCatTipoServicio")) {
                        p.setIdCatTipoServicio(parseIntSafe(el.getAttribute("idCatTipoServicio")));
                    }
                    if (el.hasAttribute("tipoFront")) {
                        p.setTipoFront(parseIntSafe(el.getAttribute("tipoFront")));
                    }
                    if (el.hasAttribute("hasDigitoVerificador")) {
                        p.setHasDigitoVerificador(Boolean.parseBoolean(el.getAttribute("hasDigitoVerificador")));
                    }
                    if (el.hasAttribute("precio")) {
                        p.setPrecio(parseDoubleSafe(el.getAttribute("precio")));
                    }
                    if (el.hasAttribute("showAyuda")) {
                        p.setShowAyuda(Boolean.parseBoolean(el.getAttribute("showAyuda")));
                    }
                    if (el.hasAttribute("tipoReferencia")) {
                        p.setTipoReferencia(el.getAttribute("tipoReferencia"));
                    }

                    NodeList legends = el.getElementsByTagName("legend");
                    if (legends.getLength() > 0) {
                        p.setLegend(legends.item(0).getTextContent().trim());
                    }

                    if (p.getIdProducto() != null) {
                        products.add(p);
                    }
                }
            }

            log.info("XML de GestoPago parseado exitosamente: {} productos encontrados", products.size());
            return products;

        } catch (Exception e) {
            log.error("Error al parsear el XML de GestoPago: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Integer parseIntSafe(String val) {
        try {
            return (val != null && !val.isBlank()) ? Integer.parseInt(val.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleSafe(String val) {
        try {
            return (val != null && !val.isBlank()) ? Double.parseDouble(val.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
