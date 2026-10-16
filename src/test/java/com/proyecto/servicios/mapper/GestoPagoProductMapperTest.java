package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Modulo Mappers - GestoPagoProductMapper")
class GestoPagoProductMapperTest {

    private final GestoPagoProductMapper mapper = Mappers.getMapper(GestoPagoProductMapper.class);

    @Test
    @DisplayName("TC-MAP-01: Mapear Entity a DTO correctamente")
    void toDto() {
        GestoPagoProduct entity = new GestoPagoProduct();
        entity.setIdProducto(1);
        entity.setProducto("Telcel");
        entity.setPrecio(100.00);

        GestoPagoProductDTO dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(1, dto.getIdProducto());
        assertEquals("Telcel", dto.getProducto());
        assertEquals(100.00, dto.getPrecio());
    }

    @Test
    @DisplayName("TC-MAP-02: Mapear nulos retorna nulo")
    void toDto_null() {
        assertNull(mapper.toDto(null));
        assertNull(mapper.toEntity(null));
        assertNull(mapper.toDtoList(null));
        assertNull(mapper.toEntityList(null));
    }
}
