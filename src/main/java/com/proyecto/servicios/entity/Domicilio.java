package com.proyecto.servicios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
@NoArgsConstructor
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", referencedColumnName = "id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Cliente cliente;

    @Column(nullable = false, length = 255)
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 50)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 50)
    private String numeroInterior;

    @Column(nullable = false, length = 255)
    private String colonia;

    @Column(nullable = false, length = 255)
    private String municipio;

    @Column(nullable = false, length = 255)
    private String estado;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;

    @Column(nullable = false, length = 100)
    private String pais;
}
