package com.atendeja.model;

import com.atendeja.enums.DesfechoAtendimento;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cpf_paciente", length = 11)
    private String cpfPaciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DesfechoAtendimento desfecho;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "tempo_espera_min", nullable = false)
    private Integer tempoEsperaMin = 0;

    @Column(name = "realizado_em", nullable = false, updatable = false)
    private LocalDateTime realizadoEm;

    @OneToOne
    @JoinColumn(name = "senha_id", nullable = false, unique = true)
    private Senha senha;

    @ManyToOne
    @JoinColumn(name = "guiche_id", nullable = false)
    private Guiche guiche;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario atendente;

    @PrePersist
    public void prePersist() {
        this.realizadoEm = LocalDateTime.now();
    }
}