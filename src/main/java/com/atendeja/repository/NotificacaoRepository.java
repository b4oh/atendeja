package com.atendeja.repository;

import com.atendeja.model.Notificacao;
import com.atendeja.enums.StatusNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findBySenhaId(Long senhaId);
    List<Notificacao> findByStatus(StatusNotificacao status);
}