package com.atendeja.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "servico")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 1)
    private Character prefixo;

    @Column(nullable = false)
    private Boolean disponivel = true;

    @Column(name = "capacidade_diaria", nullable = false)
    private Integer capacidadeDiaria = 50;

    @ManyToOne
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeSaude unidade;

    @OneToMany(mappedBy = "servico", cascade = CascadeType.ALL)
    private List<Senha> senhas;
}