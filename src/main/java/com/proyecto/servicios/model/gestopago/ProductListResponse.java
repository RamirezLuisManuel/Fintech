package com.proyecto.servicios.model.gestopago;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductListResponse {
    private boolean success;
    private String message;
    private List<ProductDTO> products;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductDTO {
        private Integer id;
        private String name;
        private String description;
        // Agrega aquí los campos específicos que devuelva Gestopago
    }
}
