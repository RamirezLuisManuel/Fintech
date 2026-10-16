package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreo(String correoElectronico);
    
    List<Cliente> findByActivoTrue();
    Page<Cliente> findByActivoTrue(Pageable pageable);
    
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreo(String correoElectronico);

    // Buscadores individuales flexibles ignorando mayusculas/minusculas
    Page<Cliente> findByRfcContainingIgnoreCase(String rfc, Pageable pageable);
    Page<Cliente> findByCurpContainingIgnoreCase(String curp, Pageable pageable);
    Page<Cliente> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<Cliente> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno, Pageable pageable);

    // Buscador universal inteligente unificado con paginacion
    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND (" +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
           "LOWER(c.segundoNombre) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
           "LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
           "LOWER(c.curp) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
           "LOWER(c.rfc) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
           "LOWER(c.correo) LIKE LOWER(CONCAT('%', :criterio, '%')))")
    Page<Cliente> buscarPorCriterio(@Param("criterio") String criterio, Pageable pageable);
}