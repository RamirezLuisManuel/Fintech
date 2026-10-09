package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreo(String correoElectronico);
    List<Cliente> findByActivoTrue();
    List<Cliente> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreo(String correoElectronico);
}
