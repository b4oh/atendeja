package com.atendeja.model;

import com.atendeja.enums.StatusNotificacao;
import com.atendeja.enums.TipoNotificacao;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = 20)
    private String destino;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotificacao status = StatusNotificacao.PENDENTE;

    @Column(name = "enviada_em", nullable = false, updatable = false)
    private LocalDateTime enviadaEm;

    @ManyToOne
    @JoinColumn(name = "senha_id", nullable = false)
    private Senha senha;

    @PrePersist
    public void prePersist() {
        this.enviadaEm = LocalDateTime.now();
    }
}