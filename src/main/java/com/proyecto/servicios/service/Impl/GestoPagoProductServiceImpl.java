package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.mapper.GestoPagoProductMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.GestoPagoXmlParser;
import com.proyecto.servicios.util.Constants;
import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    @Autowired
    private GestoPagoProductRepository productRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private GestoPagoServiceClient gestoPagoServiceClient;
    @Autowired
    private GestoPagoXmlParser xmlParser;
    @Autowired
    private GestoPagoProductMapper productMapper;
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper jsonMapper;

    @Override
    public List<GestoPagoProductDTO> getProductList() {
        List<GestoPagoProduct> products = fetchFromRedis();
        if (products != null) {
            return productMapper.toDtoList(products);
        }

        products = fetchFromPostgres();
        if (products != null) {
            return productMapper.toDtoList(products);
        }

        return productMapper.toDtoList(fetchFromGestopagoAndSave());
    }

    private List<GestoPagoProduct> fetchFromRedis() {
        try {
            Object obj = redisTemplate.opsForValue().get(Constants.REDIS_KEY_GESTOPAGO_PRODUCTS);
            if (obj != null) {
                List<GestoPagoProduct> cached = jsonMapper.convertValue(obj, new com.fasterxml.jackson.core.type.TypeReference<List<GestoPagoProduct>>() {
                });
                if (!cached.isEmpty()) {
                    log.info("Productos obtenidos exitosamente desde Redis Cache");
                    return cached;
                }
            }
        } catch (Exception e) {
            log.error("Fallo al consultar Redis: {}", e.getMessage());
        }
        return null;
    }

    private List<GestoPagoProduct> fetchFromPostgres() {
        try {
            List<GestoPagoProduct> dbProducts = productRepository.findAll();
            if (dbProducts != null && !dbProducts.isEmpty()) {
                log.info("Productos obtenidos exitosamente desde Postgres");
                redisTemplate.opsForValue().set(Constants.REDIS_KEY_GESTOPAGO_PRODUCTS, dbProducts);
                return dbProducts;
            }
        } catch (Exception e) {
            log.error("Fallo al consultar Postgres: {}", e.getMessage());
        }
        return null;
    }

    private List<GestoPagoProduct> fetchFromGestopagoAndSave() {
        log.warn("Redis y Postgres fallaron o estan vacios. Consultando directo a Gestopago...");
        try {
            Response feignResponse = gestoPagoServiceClient.getProductList();
            String rawXml = IOUtils.toString(feignResponse.body().asInputStream(), StandardCharsets.UTF_8);

            List<GestoPagoProduct> freshProducts = xmlParser.parseXml(rawXml);
            if (freshProducts == null || freshProducts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "2: Gestopago funciona pero devolvio XML invalido");
            }

            productRepository.saveAll(freshProducts);
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_GESTOPAGO_PRODUCTS, freshProducts);

            log.info("Productos obtenidos directamente de Gestopago y guardados (Fallback completado)");
            return freshProducts;

        } catch (RetryableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "1: No hay conexion a GestoPago.");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "2: Error de conexion (Respuesta invalida de Gestopago).");
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            log.error("Error al contactar Gestopago", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado obteniendo productos");
        }
    }
}
