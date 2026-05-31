package com.atendeja.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tipo_exame")
public class TipoExame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos = 30;

    @Column(name = "vagas_por_dia", nullable = false)
    private Integer vagasPorDia = 20;

    @Column(nullable = false)
    private Boolean disponivel = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeSaude unidade;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}