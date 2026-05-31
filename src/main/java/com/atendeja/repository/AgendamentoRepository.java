package com.atendeja.repository;

import com.atendeja.model.Agendamento;
import com.atendeja.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByUsuarioIdOrderByDataExameDescHoraExameDesc(Long usuarioId);
    List<Agendamento> findByUnidadeIdAndDataExameOrderByHoraExameAsc(Long unidadeId, LocalDate data);
    List<Agendamento> findByUnidadeIdOrderByDataExameDescHoraExameDesc(Long unidadeId);
    long countByTipoExameIdAndDataExameAndStatusNot(Long tipoExameId, LocalDate data, StatusAgendamento status);
    List<Agendamento> findByTipoExameIdAndDataExame(Long tipoExameId, LocalDate dataExame);
}