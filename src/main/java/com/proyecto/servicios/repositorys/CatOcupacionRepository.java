package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.CatOcupacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatOcupacionRepository extends JpaRepository<CatOcupacion, Integer> {
    List<CatOcupacion> findAllByOrderByIdAsc();
}
