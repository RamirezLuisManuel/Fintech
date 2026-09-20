package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.GestoPagoXmlParser;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private final GestoPagoProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GestoPagoServiceClient gestoPagoServiceClient;
    private final GestoPagoXmlParser xmlParser;

    public GestoPagoProductServiceImpl(GestoPagoProductRepository productRepository, 
                                       RedisTemplate<String, Object> redisTemplate,
                                       GestoPagoServiceClient gestoPagoServiceClient,
                                       GestoPagoXmlParser xmlParser) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.gestoPagoServiceClient = gestoPagoServiceClient;
        this.xmlParser = xmlParser;
    }

    @Override
    public List<GestoPagoProduct> getProductList() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        // 1. Try Redis
        try {
            Object obj = redisTemplate.opsForValue().get("gestopago_products");
            if (obj != null) {
                List<GestoPagoProduct> redisProducts = mapper.convertValue(obj, new com.fasterxml.jackson.core.type.TypeReference<List<GestoPagoProduct>>() {});
                if (!redisProducts.isEmpty()) {
                    log.info("Productos obtenidos exitosamente desde Redis Cache");
                    return redisProducts;
                }
            }
        } catch (Exception e) {
            log.error("Fallo al consultar Redis: {}", e.getMessage());
        }

        // 2. Try Postgres
        try {
            List<GestoPagoProduct> dbProducts = productRepository.findAll();
            if (dbProducts != null && !dbProducts.isEmpty()) {
                log.info("Productos obtenidos exitosamente desde Postgres");
                // Repopulate Redis
                try {
                    redisTemplate.opsForValue().set("gestopago_products", dbProducts);
                } catch (Exception ignored) {}
                return dbProducts;
            }
        } catch (Exception e) {
            log.error("Fallo al consultar Postgres: {}", e.getMessage());
        }

        // 3. Try Gestopago directly
        log.warn("Redis y Postgres fallaron o estan vacios. Consultando directo a Gestopago...");
        try {
            String rawXml = gestoPagoServiceClient.getProductList();
            List<GestoPagoProduct> freshProducts = xmlParser.parseXml(rawXml);
            if (freshProducts == null || freshProducts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "2: Gestopago funciona pero devolvio una lista vacia o un XML invalido");
            }
            log.info("Productos obtenidos directamente de Gestopago (Fallback completado)");
            return freshProducts;
        } catch (RetryableException e) {
            // Error de timeout o conexion de Feign
            log.error("No hay conexion a Gestopago: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "1: No hay conexion a GestoPago. Intente mas tarde.");
        } catch (FeignException e) {
            log.error("Gestopago retorno error de conexion/http: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "2: Ocurrio un error de conexion (Respuesta invalida de Gestopago).");
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) throw e;
            log.error("Error inesperado al contactar Gestopago: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado obteniendo productos");
        }
    }
}
