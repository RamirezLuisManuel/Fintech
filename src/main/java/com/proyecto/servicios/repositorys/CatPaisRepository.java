package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.CatPais;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatPaisRepository extends JpaRepository<CatPais, Integer> {
    List<CatPais> findAllByOrderByIdAsc();
}
