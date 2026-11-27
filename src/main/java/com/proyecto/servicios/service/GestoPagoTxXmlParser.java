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

    public static final String IP_BLOCKED_CODE = "05";
    public static final String IP_BLOCKED_MESSAGE =
            "Acceso denegado por GestoPago (Código 05): la dirección IP de origen no está autorizada para operar.";

    public static boolean isIpValidationFailure(String codigo, String texto) {
        if ("05".equals(codigo) || "55".equals(codigo)) {
            return true;
        }
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String lower = texto.toLowerCase();
        return lower.contains("validationcompleted")
                || lower.contains("validips")
                || lower.contains("isvalid=false")
                || lower.contains("codigodispositivo=")
                || lower.contains("iddistribuidor=")
                || lower.contains("ip no autorizada")
                || lower.contains("ip bloqueada");
    }

    public static GestoPagoTxResponse normalizeResponse(GestoPagoTxResponse response) {
        if (response == null) {
            return null;
        }
        if (isIpValidationFailure(response.getCodigo(), response.getTexto())) {
            response.setCodigo(IP_BLOCKED_CODE);
            response.setTexto(IP_BLOCKED_MESSAGE);
        }
        return response;
    }

    public static GestoPagoTxResponse buildIpBlockedResponse() {
        return GestoPagoTxResponse.builder()
                .codigo(IP_BLOCKED_CODE)
                .texto(IP_BLOCKED_MESSAGE)
                .build();
    }

    public GestoPagoTxResponse parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("GestoPago devolvio una respuesta vacia");
        }
        if (isIpValidationFailure(null, xml) && !xml.trim().startsWith("<")) {
            return buildIpBlockedResponse();
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
            GestoPagoTxResponse parsed = GestoPagoTxResponse.builder()
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
            return normalizeResponse(parsed);
        } catch (Exception ex) {
            if (isIpValidationFailure(null, xml)) {
                return buildIpBlockedResponse();
            }
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
