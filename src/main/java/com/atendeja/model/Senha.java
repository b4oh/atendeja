package com.atendeja.model;

import com.atendeja.enums.CanalEmissao;
import com.atendeja.enums.StatusSenha;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "senha")
public class Senha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_emissao", nullable = false, length = 20)
    private CanalEmissao canalEmissao = CanalEmissao.PORTAL_WEB;

    @Column(nullable = false)
    private Boolean prioritaria = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSenha status = StatusSenha.AGUARDANDO;

    @Column(length = 15)
    private String celular;

    @Column(name = "emitida_em", nullable = false, updatable = false)
    private LocalDateTime emitidaEm;

    @Column(name = "chamada_em")
    private LocalDateTime chamadaEm;

    @ManyToOne
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeSaude unidade;

    @ManyToOne
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @OneToOne(mappedBy = "senha", cascade = CascadeType.ALL)
    private Atendimento atendimento;

    @OneToMany(mappedBy = "senha", cascade = CascadeType.ALL)
    private List<Notificacao> notificacoes;

    @PrePersist
    public void prePersist() {
        this.emitidaEm = LocalDateTime.now();
    }
}
