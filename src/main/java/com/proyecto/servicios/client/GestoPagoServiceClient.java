package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.GestoPagoFeignConfig;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import feign.Response;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "gestoPagoServiceClient", url = "${gestopago.service.url}", configuration = GestoPagoFeignConfig.class)
public interface GestoPagoServiceClient {

    @GetMapping("/sistema/service/getProductList.do")
    Response getProductList();

}
