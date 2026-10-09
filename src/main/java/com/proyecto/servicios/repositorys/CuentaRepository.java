package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    boolean existsByNumeroCuenta(String numeroCuenta);
    List<Cuenta> findByClienteId(Long clienteId);
    List<Cuenta> findByActivoTrue();
}
