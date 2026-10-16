package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.model.request.cliente.ClienteBusquedaDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Especificacion dinamica para consultas avanzadas usando Criteria API de JPA.
 * Permite filtrar por multiples campos opcionales sin armar SQL concatenado rigido.
 */
public class ClienteSpecification {

    public static Specification<Cliente> conFiltros(ClienteBusquedaDTO filtros) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtros == null) {
                return cb.isTrue(root.get("activo"));
            }

            if (filtros.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtros.getActivo()));
            } else {
                predicates.add(cb.isTrue(root.get("activo")));
            }

            if (filtros.getCriterio() != null && !filtros.getCriterio().trim().isEmpty()) {
                String pattern = "%" + filtros.getCriterio().trim().toLowerCase() + "%";
                Predicate porNombre = cb.like(cb.lower(root.get("nombre")), pattern);
                Predicate porPaterno = cb.like(cb.lower(root.get("apellidoPaterno")), pattern);
                Predicate porRfc = cb.like(cb.lower(root.get("rfc")), pattern);
                Predicate porCurp = cb.like(cb.lower(root.get("curp")), pattern);
                Predicate porCorreo = cb.like(cb.lower(root.get("correo")), pattern);
                predicates.add(cb.or(porNombre, porPaterno, porRfc, porCurp, porCorreo));
            }

            if (filtros.getRfc() != null && !filtros.getRfc().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("rfc")), "%" + filtros.getRfc().trim().toLowerCase() + "%"));
            }

            if (filtros.getCurp() != null && !filtros.getCurp().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("curp")), "%" + filtros.getCurp().trim().toLowerCase() + "%"));
            }

            if (filtros.getNombre() != null && !filtros.getNombre().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtros.getNombre().trim().toLowerCase() + "%"));
            }

            if (filtros.getApellidoPaterno() != null && !filtros.getApellidoPaterno().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + filtros.getApellidoPaterno().trim().toLowerCase() + "%"));
            }

            if (filtros.getCorreo() != null && !filtros.getCorreo().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("correo")), "%" + filtros.getCorreo().trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Cliente> conFiltros(String criterio, String rfc, String curp, String nombre, Boolean activo) {
        ClienteBusquedaDTO dto = new ClienteBusquedaDTO();
        dto.setCriterio(criterio);
        dto.setRfc(rfc);
        dto.setCurp(curp);
        dto.setNombre(nombre);
        dto.setActivo(activo);
        return conFiltros(dto);
    }
}