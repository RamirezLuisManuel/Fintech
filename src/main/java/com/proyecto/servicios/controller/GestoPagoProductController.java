package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import com.proyecto.servicios.service.GestoPagoProductService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private GestoPagoProductService gestoPagoProductService;

    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GestoPagoProductDTO>> getProductList() {
        List<GestoPagoProductDTO> response = gestoPagoProductService.getProductList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
