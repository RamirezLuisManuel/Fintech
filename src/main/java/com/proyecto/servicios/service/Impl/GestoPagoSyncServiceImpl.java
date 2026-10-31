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
import java.time.Duration;

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
        long localCount = productRepository.count();
        if (localCount == 0) {
            log.info("Catalogo local vacio (0 productos); ejecutando sincronizacion inicial de GestoPago...");
            syncProducts();
        } else {
            log.info("Catalogo local existente ({} productos); calentando cache Redis...", localCount);
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_GESTOPAGO_PRODUCTS,
                    productRepository.findAll(), Duration.ofHours(25));
        }
    }

    @Scheduled(cron = "${gestopago.sync.cron:0 0 0 * * ?}")
    public void syncProducts() {
        log.info("Iniciando sincronizacion nocturna (media noche) de productos Gestopago...");
        try {
            Response feignResponse = gestoPagoServiceClient.getProductList();
            String rawXml = IOUtils.toString(feignResponse.body().asInputStream(), StandardCharsets.UTF_8);

            List<GestoPagoProduct> productos = xmlParser.parseXml(rawXml);
            if (productos == null || productos.isEmpty()) {
                log.warn("Sincronizacion ignorada: GestoPago devolvio un catalogo vacio.");
                return;
            }

            long localCount = productRepository.count();
            if (productos.size() > localCount) {
                saveToDatabases(productos);
                log.info("Sincronizacion exitosa: catalogo remoto ({}) es mayor al local ({}). Guardado en BD y Redis.",
                        productos.size(), localCount);
            } else {
                log.info("Sincronizacion ignorada: catalogo remoto ({}) es igual o menor al local ({}). No se sobreescribe en BD.",
                        productos.size(), localCount);
                redisTemplate.opsForValue().set(Constants.REDIS_KEY_GESTOPAGO_PRODUCTS,
                        productRepository.findAll(), Duration.ofHours(25));
            }
        } catch (Exception e) {
            log.error("Fallo la sincronizacion nocturna de productos GestoPago: {}", e.getMessage(), e);
        }
    }

    private void saveToDatabases(List<GestoPagoProduct> productos) {
        productRepository.saveAll(productos);
        redisTemplate.opsForValue().set(Constants.REDIS_KEY_GESTOPAGO_PRODUCTS, productos, Duration.ofHours(25));
    }
}
