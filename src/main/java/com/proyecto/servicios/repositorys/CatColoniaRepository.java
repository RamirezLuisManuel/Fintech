package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.CatColonia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatColoniaRepository extends JpaRepository<CatColonia, Integer> {
    List<CatColonia> findByMunicipioIdOrderByNombreAsc(Integer municipioId);
}
