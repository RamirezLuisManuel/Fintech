package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.GestoPagoFeignConfig;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "gestoPagoServiceClient", url = "${gestopago.service.url}", configuration = GestoPagoFeignConfig.class)
public interface GestoPagoServiceClient {

    @GetMapping("/sistema/service/getProductList.do")
    String getProductList();

}
