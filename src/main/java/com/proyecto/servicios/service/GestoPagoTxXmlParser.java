package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;

@Service
public class GestoPagoTxXmlParser {

    public GestoPagoTxResponse parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("GestoPago devolvio una respuesta vacia");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            Element message = firstElement(root, "MENSAJE");

            String rootId = text(root, "ID_TX");
            String messageId = message == null ? null : text(message, "ID_TX");
            return GestoPagoTxResponse.builder()
                    .codigo(message == null ? null : text(message, "CODIGO"))
                    .texto(message == null ? null : text(message, "TEXTO"))
                    .idTx(rootId != null ? rootId : messageId)
                    .numeroAutorizacion(text(root, "NUM_AUTORIZACION"))
                    .saldo(decimal(text(root, "SALDO")))
                    .comision(decimal(text(root, "COMISION")))
                    .monto(decimal(text(root, "MONTO")))
                    .saldoReferencia(message == null ? null : decimal(text(message, "SALDO")))
                    .referencia(message == null ? null : text(message, "REFERENCIA"))
                    .pin(message == null ? null : text(message, "PIN"))
                    .legend(message == null ? null : text(message, "legend"))
                    .build();
        } catch (Exception ex) {
            throw new IllegalArgumentException("La respuesta XML de GestoPago no es valida", ex);
        }
    }

    private Element firstElement(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        String value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal decimal(String value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
