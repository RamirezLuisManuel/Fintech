package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaction;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GestoPagoTransactionRepository extends JpaRepository<GestoPagoTransaction, UUID> {

    @EntityGraph(attributePaths = {"usuario", "cliente", "cuenta", "cuenta.cliente"})
    Optional<GestoPagoTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByUpc(String upc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from GestoPagoTransaction t where t.id = :id")
    Optional<GestoPagoTransaction> findByIdForUpdate(@Param("id") UUID id);

    @Query("select t.id from GestoPagoTransaction t " +
            "where t.estado in :statuses and t.nextConfirmationAt <= :now order by t.nextConfirmationAt")
    List<UUID> findIdsReadyForConfirmation(
            @Param("statuses") Collection<GestoPagoTransactionStatus> statuses,
            @Param("now") LocalDateTime now);
}
