package com.atendeja.repository;

import com.atendeja.model.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    List<Atendimento> findByGuicheIdAndRealizadoEmBetween(
            Long guicheId, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT AVG(a.tempoEsperaMin) FROM Atendimento a " +
            "WHERE a.guiche.unidade.id = :unidadeId " +
            "AND CAST(a.realizadoEm AS date) = CURRENT_DATE")
    Double findTempoMedioEsperaHoje(Long unidadeId);
}