package com.atendeja.model;

import com.atendeja.enums.StatusGuiche;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "guiche")
public class Guiche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusGuiche status = StatusGuiche.FECHADO;

    @ManyToOne
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeSaude unidade;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario operador;
}