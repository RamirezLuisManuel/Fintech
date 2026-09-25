package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.GestoPagoXmlParser;
import com.proyecto.servicios.util.Constants;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service

@Slf4j
public class GestoPagoSyncServiceImpl {

    @Autowired
    private GestoPagoServiceClient gestoPagoServiceClient;
    @Autowired
    private GestoPagoXmlParser xmlParser;
    @Autowired
    private GestoPagoProductRepository productRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @jakarta.annotation.PostConstruct
    public void initSync() {
        log.info("Ejecutando sincronizacion inicial al arrancar la aplicacion...");
        syncProducts();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void syncProducts() {
        log.info("Iniciando sincronizacion nocturna de productos Gestopago...");
        try {
            Response feignResponse = gestoPagoServiceClient.getProductList();
            String rawXml = IOUtils.toString(feignResponse.body().asInputStream(), StandardCharsets.UTF_8);

            List<GestoPagoProduct> productos = xmlParser.parseXml(rawXml);
            long oldSize = productRepository.count();

            if (productos != null && productos.size() > oldSize) {
                saveToDatabases(productos);
                log.info("Sincronizacion de GestoPago finalizada. Productos cacheados: {}", productos.size());
            } else {
                log.warn("Sincronizacion ignorada: El nuevo tamano ({}) no es mayor al anterior ({}).",
                        (productos != null ? productos.size() : 0), oldSize);
            }
        } catch (Exception e) {
            log.error("Fallo la sincronizacion nocturna de productos GestoPago: {}", e.getMessage(), e);
        }
    }

    private void saveToDatabases(List<GestoPagoProduct> productos) {
        productRepository.saveAll(productos);
        redisTemplate.opsForValue().set(Constants.REDIS_KEY_GESTOPAGO_PRODUCTS, productos);
    }
}
