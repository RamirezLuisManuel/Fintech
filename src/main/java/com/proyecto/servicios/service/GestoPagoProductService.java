package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import java.util.List;

public interface GestoPagoProductService {
    List<GestoPagoProductDTO> getProductList();
}
