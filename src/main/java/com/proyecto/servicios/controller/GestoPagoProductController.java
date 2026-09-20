package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.service.GestoPagoProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gestopago")
public class GestoPagoProductController {

    private final GestoPagoProductService gestoPagoProductService;

    public GestoPagoProductController(GestoPagoProductService gestoPagoProductService) {
        this.gestoPagoProductService = gestoPagoProductService;
    }

    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GestoPagoProduct>> getProductList() {
        List<GestoPagoProduct> response = gestoPagoProductService.getProductList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
