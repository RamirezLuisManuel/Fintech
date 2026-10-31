package com.proyecto.servicios.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestoPagoEncryptionServiceTest {

    private static final String KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    @Test
    void encrypt_matchesOfficialDocumentationVector() {
        GestoPagoEncryptionService service = new GestoPagoEncryptionService(KEY, IV);
        String payload = "{'idProducto':582,'idServicio':133,'telefono':'5555555551'," +
                "'horaLocal':'20220101235900','upc':'test_UPC_83_182'}";

        assertEquals(
                "+LWZ7icI60/DAI8ggv/SpDvJpFYeaYOZ5J7hPGK7g/87r9PRJfgFRBoBLYpBxW33" +
                        "ouvuwQPnhhKHXqzbiqLhMSH4gSH8SMvW6PvHfeklNfTrZnbgWFWDPSy/nqFCP0B/" +
                        "gKavafHG0tW5UP4KPwyhECZK2pLT+I3qYAA85uQaAPQ=",
                service.encrypt(payload));
    }

    @Test
    void constructor_rejectsInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new GestoPagoEncryptionService("dGVzdA==", IV));
    }
}
