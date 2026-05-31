package com.atendeja.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "log_acesso")
public class LogAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 45)
    private String ip;

    @Column(nullable = false)
    private Boolean sucesso;

    @Column(name = "acessado_em", nullable = false, updatable = false)
    private LocalDateTime acessadoEm;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    public void prePersist() {
        this.acessadoEm = LocalDateTime.now();
    }
}