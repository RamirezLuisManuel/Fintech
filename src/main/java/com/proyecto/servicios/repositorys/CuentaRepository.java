package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    boolean existsByNumeroCuenta(String numeroCuenta);
    List<Cuenta> findByClienteId(Long clienteId);
    List<Cuenta> findByActivoTrue();

    @Modifying
    @Query(value = "UPDATE cuentas SET saldo_retenido = saldo_retenido + :monto, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :cuentaId AND activo = TRUE AND (saldo - saldo_retenido) >= :monto", nativeQuery = true)
    int reservarSaldo(@Param("cuentaId") Long cuentaId, @Param("monto") java.math.BigDecimal monto);

    @Modifying
    @Query(value = "UPDATE cuentas SET saldo = saldo - :monto, saldo_retenido = saldo_retenido - :monto, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = :cuentaId AND saldo_retenido >= :monto", nativeQuery = true)
    int capturarSaldoRetenido(@Param("cuentaId") Long cuentaId, @Param("monto") java.math.BigDecimal monto);

    @Modifying
    @Query(value = "UPDATE cuentas SET saldo_retenido = saldo_retenido - :monto, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :cuentaId AND saldo_retenido >= :monto", nativeQuery = true)
    int liberarSaldoRetenido(@Param("cuentaId") Long cuentaId, @Param("monto") java.math.BigDecimal monto);
}
