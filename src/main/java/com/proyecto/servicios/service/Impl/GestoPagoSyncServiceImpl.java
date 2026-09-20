package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.GestoPagoXmlParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GestoPagoSyncServiceImpl {

    private final GestoPagoServiceClient gestoPagoServiceClient;
    private final GestoPagoXmlParser xmlParser;
    private final GestoPagoProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public GestoPagoSyncServiceImpl(GestoPagoServiceClient gestoPagoServiceClient,
                                    GestoPagoXmlParser xmlParser,
                                    GestoPagoProductRepository productRepository,
                                    RedisTemplate<String, Object> redisTemplate) {
        this.gestoPagoServiceClient = gestoPagoServiceClient;
        this.xmlParser = xmlParser;
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    @jakarta.annotation.PostConstruct
    public void initSync() {
        log.info("Ejecutando sincronizacion inicial al arrancar la aplicacion...");
        syncProducts();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void syncProducts() {
        log.info("Iniciando sincronizacion nocturna de productos Gestopago...");
        try {
            String rawXml = gestoPagoServiceClient.getProductList();
            List<GestoPagoProduct> productos = xmlParser.parseXml(rawXml);
            
            long oldSize = productRepository.count();
            
            if (productos != null && productos.size() > oldSize) {
                // Guarda en Postgres
                productRepository.saveAll(productos);
                // Guarda en Redis
                redisTemplate.opsForValue().set("gestopago_products", productos);
                log.info("Sincronizacion de GestoPago finalizada. Productos cacheados en Postgres y Redis: {}", productos.size());
            } else {
                log.warn("Sincronizacion ignorada: El nuevo tamano ({}) no es mayor al anterior ({}).", 
                    (productos != null ? productos.size() : 0), oldSize);
            }
        } catch (Exception e) {
            log.error("Fallo la sincronizacion nocturna de productos GestoPago: {}", e.getMessage(), e);
        }
    }
}
