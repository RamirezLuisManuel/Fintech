package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListXml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
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
            // Limpiamos el XML anómalo de GestoPago
            String cleanXml = rawXml.replaceAll("(?i)<\\?xml[^>]*\\?>", "").trim();
            String wrappedXml = "<root>" + cleanXml + "</root>";

            JAXBContext jaxbContext = JAXBContext.newInstance(GestoPagoProductListXml.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            GestoPagoProductListXml parsedRoot = (GestoPagoProductListXml) unmarshaller.unmarshal(new StringReader(wrappedXml));
            return parsedRoot.getProductos() != null ? parsedRoot.getProductos() : new ArrayList<>();

        } catch (Exception e) {
            log.error("Error al parsear el XML de GestoPago usando JAXB: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
