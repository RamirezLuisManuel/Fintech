package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.CatEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatEstadoRepository extends JpaRepository<CatEstado, Integer> {
    List<CatEstado> findByPaisIdOrderByNombreAsc(Integer paisId);
}
