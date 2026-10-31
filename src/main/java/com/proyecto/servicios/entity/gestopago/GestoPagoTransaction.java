package com.proyecto.servicios.entity.gestopago;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gestopago_transactions")
@Getter
@Setter
@NoArgsConstructor
public class GestoPagoTransaction {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, unique = true, length = 15)
    private String upc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "tipo_front", nullable = false)
    private Integer tipoFront;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 100)
    private String referencia;

    @Column(nullable = false, precision = 24, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 25)
    private String unidad;

    @Column(name = "hora_local", length = 14)
    private String horaLocal;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GestoPagoTransactionStatus estado;

    @Column(name = "provider_code", length = 10)
    private String providerCode;

    @Column(name = "provider_message", columnDefinition = "TEXT")
    private String providerMessage;

    @Column(name = "provider_transaction_id", length = 60)
    private String providerTransactionId;

    @Column(name = "provider_authorization", length = 60)
    private String providerAuthorization;

    @Column(length = 500)
    private String pin;

    @Column(columnDefinition = "TEXT")
    private String legend;

    @Column(name = "confirmation_attempts", nullable = false)
    private Integer confirmationAttempts = 0;

    @Column(name = "next_confirmation_at")
    private LocalDateTime nextConfirmationAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
