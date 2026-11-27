package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.CatMunicipio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatMunicipioRepository extends JpaRepository<CatMunicipio, Integer> {
    List<CatMunicipio> findByEstadoIdOrderByNombreAsc(Integer estadoId);
}
