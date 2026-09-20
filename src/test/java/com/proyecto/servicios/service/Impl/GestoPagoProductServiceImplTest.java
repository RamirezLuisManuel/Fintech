package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.GestoPagoXmlParser;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductServiceImplTest {

    @Mock
    private GestoPagoProductRepository productRepository;

    @Mock
    private GestoPagoServiceClient gestoPagoServiceClient;

    @Mock
    private GestoPagoXmlParser xmlParser;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private GestoPagoProductServiceImpl productService;

    private List<GestoPagoProduct> mockProductList;

    @BeforeEach
    void setUp() {
        GestoPagoProduct product = new GestoPagoProduct();
        product.setIdProducto(123);
        product.setProducto("Recarga Telcel 50");
        product.setPrecio(50.0);
        mockProductList = List.of(product);
        
        // Mock default leniency for Redis ops
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetProductList_SuccessFromRedis() {
        // Arrange
        when(valueOperations.get("gestopago_products")).thenReturn(mockProductList);

        // Act
        List<GestoPagoProduct> result = productService.getProductList();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Recarga Telcel 50", result.get(0).getProducto());
        verify(productRepository, never()).findAll();
        verify(gestoPagoServiceClient, never()).getProductList();
    }

    @Test
    void testGetProductList_FallbackToPostgres() {
        // Arrange
        when(valueOperations.get("gestopago_products")).thenReturn(null); // Redis vacio
        when(productRepository.findAll()).thenReturn(mockProductList); // Postgres con datos

        // Act
        List<GestoPagoProduct> result = productService.getProductList();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(productRepository, times(1)).findAll();
        verify(gestoPagoServiceClient, never()).getProductList();
        verify(valueOperations, times(1)).set("gestopago_products", mockProductList); // Repoblar redis
    }

    @Test
    void testGetProductList_FallbackToApiSuccess() throws Exception {
        // Arrange
        when(valueOperations.get("gestopago_products")).thenReturn(null);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        
        String dummyXml = "<root></root>";
        when(gestoPagoServiceClient.getProductList()).thenReturn(dummyXml);
        when(xmlParser.parseXml(dummyXml)).thenReturn(mockProductList);

        // Act
        List<GestoPagoProduct> result = productService.getProductList();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(gestoPagoServiceClient, times(1)).getProductList();
    }

    @Test
    void testGetProductList_ApiTimeoutThrowsCodigo1() {
        // Arrange
        when(valueOperations.get("gestopago_products")).thenReturn(null);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        
        Request mockRequest = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        when(gestoPagoServiceClient.getProductList()).thenThrow(new RetryableException(500, "Timeout", Request.HttpMethod.GET, new Date(), mockRequest));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            productService.getProductList();
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertTrue(exception.getReason().contains("1:")); // Codigo 1
    }

    @Test
    void testGetProductList_ApiErrorThrowsCodigo2() {
        // Arrange
        when(valueOperations.get("gestopago_products")).thenReturn(null);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        
        Request mockRequest = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        when(gestoPagoServiceClient.getProductList()).thenThrow(new FeignException.BadGateway("Bad Gateway", mockRequest, null, null));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            productService.getProductList();
        });

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getReason().contains("2:")); // Codigo 2
    }
}
