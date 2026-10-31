package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoTxXmlParserTest {

    private final GestoPagoTxXmlParser parser = new GestoPagoTxXmlParser();

    @Test
    void parse_readsSuccessfulSendTxResponse() {
        String xml = """
                <RESPONSE>
                  <ID_TX>291437188</ID_TX>
                  <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>
                  <SALDO>157731.83</SALDO>
                  <COMISION>0.7</COMISION>
                  <MONTO>10.0</MONTO>
                  <MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>
                </RESPONSE>
                """;

        GestoPagoTxResponse response = parser.parse(xml);

        assertTrue(response.isSuccessfulSendTx());
        assertEquals("291437188", response.getIdTx());
        assertEquals("100024988", response.getNumeroAutorizacion());
        assertEquals(new BigDecimal("10.0"), response.getMonto());
    }

    @Test
    void parse_readsConfirmTxIdInsideMessage() {
        String xml = """
                <RESPONSE>
                  <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>
                  <MENSAJE><CODIGO>06</CODIGO><TEXTO>Aplicada</TEXTO><ID_TX>291437188</ID_TX></MENSAJE>
                </RESPONSE>
                """;

        GestoPagoTxResponse response = parser.parse(xml);

        assertTrue(response.isConfirmedAsApplied());
        assertEquals("291437188", response.getIdTx());
    }

    @Test
    void parse_rejectsDoctype() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("<!DOCTYPE foo [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><RESPONSE>&xxe;</RESPONSE>"));
    }
}
