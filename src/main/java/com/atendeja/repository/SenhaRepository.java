package com.atendeja.repository;

import com.atendeja.model.Senha;
import com.atendeja.enums.StatusSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SenhaRepository extends JpaRepository<Senha, Long> {

    List<Senha> findByUnidadeIdAndStatusOrderByPrioritariaDescEmitidaEmAsc(
            Long unidadeId, StatusSenha status);

    Optional<Senha> findByNumeroAndUnidadeId(String numero, Long unidadeId);

    @Query("SELECT COUNT(s) FROM Senha s WHERE s.servico.id = :servicoId " +
            "AND CAST(s.emitidaEm AS date) = CURRENT_DATE " +
            "AND s.status != com.atendeja.enums.StatusSenha.CANCELADA")
    Integer countSenhasHojePorServico(@Param("servicoId") Long servicoId);

    @Query("SELECT COUNT(s) FROM Senha s WHERE s.unidade.id = :unidadeId " +
            "AND s.status = com.atendeja.enums.StatusSenha.AGUARDANDO " +
            "AND s.servico.id = :servicoId " +
            "AND s.emitidaEm < :emitidaEm")
    Integer findPosicaoNaFila(
            @Param("unidadeId") Long unidadeId,
            @Param("servicoId") Long servicoId,
            @Param("emitidaEm") LocalDateTime emitidaEm);
}