package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.GestoPagoFeignConfig;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

@FeignClient(name = "gestoPagoServiceClient", url = "${gestopago.service.url}", configuration = GestoPagoFeignConfig.class)
public interface GestoPagoServiceClient {

    @GetMapping("/sistema/service/getProductList.do")
    Response getProductList();

    @PostMapping(value = "/sistema/service/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Response sendTx(@RequestBody MultiValueMap<String, String> form);

    @PostMapping(value = "/sistema/service/confirmTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Response confirmTx(@RequestBody MultiValueMap<String, String> form);

    @PostMapping(value = "/sistema/service/verifyReference.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Response verifyReference(@RequestBody MultiValueMap<String, String> form);

}
