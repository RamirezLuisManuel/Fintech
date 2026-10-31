package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoXmlParserTest {

    private GestoPagoXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new GestoPagoXmlParser();
    }

    @Test
    @DisplayName("Parse XML oficial con formato RESPONSE y PRODUCTOS")
    void parseXml_formatoOficialResponse() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<RESPONSE>" +
                "  <MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>" +
                "  <PRODUCTOS>" +
                "    <producto servicio=\"ABIB\" producto=\"ABIB 100\" idServicio=\"2284\" idProducto=\"14302\" " +
                "              idCatTipoServicio=\"13\" tipoFront=\"1\" hasDigitoVerificador=\"false\" " +
                "              precio=\"100.0\" showAyuda=\"false\" tipoReferencia=\"a\">" +
                "      <legend><![CDATA[Soporte al *787]]></legend>" +
                "    </producto>" +
                "    <producto servicio=\"CFE\" producto=\"Pago CFE\" idServicio=\"166\" idProducto=\"7357\" " +
                "              idCatTipoServicio=\"5\" tipoFront=\"4\" hasDigitoVerificador=\"true\" " +
                "              precio=\"0.0\" showAyuda=\"true\" tipoReferencia=\"b\">" +
                "      <legend>Pago de Luz</legend>" +
                "    </producto>" +
                "  </PRODUCTOS>" +
                "</RESPONSE>";

        List<GestoPagoProduct> products = parser.parseXml(xml);
        assertNotNull(products);
        assertEquals(2, products.size());

        GestoPagoProduct p1 = products.get(0);
        assertEquals(14302, p1.getIdProducto());
        assertEquals("ABIB", p1.getServicio());
        assertEquals("ABIB 100", p1.getProducto());
        assertEquals(2284, p1.getIdServicio());
        assertEquals(13, p1.getIdCatTipoServicio());
        assertEquals(1, p1.getTipoFront());
        assertFalse(p1.getHasDigitoVerificador());
        assertEquals(100.0, p1.getPrecio());
        assertFalse(p1.getShowAyuda());
        assertEquals("a", p1.getTipoReferencia());
        assertEquals("Soporte al *787", p1.getLegend());

        GestoPagoProduct p2 = products.get(1);
        assertEquals(7357, p2.getIdProducto());
        assertEquals(4, p2.getTipoFront());
        assertTrue(p2.getHasDigitoVerificador());
        assertEquals("Pago de Luz", p2.getLegend());
    }

    @Test
    @DisplayName("Parse XML vacio o nulo retorna lista vacia")
    void parseXml_vacioRetornaListaVacia() {
        assertTrue(parser.parseXml(null).isEmpty());
        assertTrue(parser.parseXml("").isEmpty());
        assertTrue(parser.parseXml("   ").isEmpty());
    }
}

