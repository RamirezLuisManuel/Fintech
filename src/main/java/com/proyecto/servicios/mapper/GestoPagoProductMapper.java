package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GestoPagoProductMapper {
    GestoPagoProductDTO toDto(GestoPagoProduct entity);
    GestoPagoProduct toEntity(GestoPagoProductDTO dto);
    List<GestoPagoProductDTO> toDtoList(List<GestoPagoProduct> entities);
    List<GestoPagoProduct> toEntityList(List<GestoPagoProductDTO> dtos);
}
