package com.proyecto.servicios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "numero_cuenta", nullable = false, unique = true)
    private String numeroCuenta;
    
    @Column(name = "refresh_token")
    private String refreshToken;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
