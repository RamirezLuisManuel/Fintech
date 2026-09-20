package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.ProductListResponse;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import java.util.List;

public interface GestoPagoProductService {
    
    List<GestoPagoProduct> getProductList();

}
