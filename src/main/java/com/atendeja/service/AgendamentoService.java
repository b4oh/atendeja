package com.atendeja.service;

import com.atendeja.enums.StatusAgendamento;
import com.atendeja.model.Agendamento;
import com.atendeja.model.TipoExame;
import com.atendeja.model.UnidadeSaude;
import com.atendeja.model.Usuario;
import com.atendeja.repository.AgendamentoRepository;
import com.atendeja.repository.TipoExameRepository;
import com.atendeja.repository.UnidadeSaudeRepository;
import com.atendeja.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private TipoExameRepository tipoExameRepository;

    @Autowired
    private UnidadeSaudeRepository unidadeSaudeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Gera os horários disponíveis para um exame numa data
    public List<LocalTime> gerarHorariosDisponiveis(Long tipoExameId, LocalDate data) {
        TipoExame exame = tipoExameRepository.findById(tipoExameId)
                .orElseThrow(() -> new RuntimeException("Exame não encontrado."));

        LocalTime inicio = LocalTime.of(7, 0);
        LocalTime fim = LocalTime.of(17, 0);
        int duracao = exame.getDuracaoMinutos();

        // Gera todos os horários possíveis
        List<LocalTime> todosHorarios = new ArrayList<>();
        LocalTime atual = inicio;
        while (atual.plusMinutes(duracao).isBefore(fim) || atual.plusMinutes(duracao).equals(fim)) {
            todosHorarios.add(atual);
            atual = atual.plusMinutes(duracao);
        }

        // Busca horários já ocupados nesta data
        List<LocalTime> ocupados = agendamentoRepository
                .findByTipoExameIdAndDataExame(tipoExameId, data).stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .map(Agendamento::getHoraExame)
                .collect(Collectors.toList());

        // Retorna apenas os disponíveis
        return todosHorarios.stream()
                .filter(h -> !ocupados.contains(h))
                .collect(Collectors.toList());
    }

    public Agendamento agendar(Long usuarioId, Long tipoExameId, Long unidadeId,
                               LocalDate dataExame, LocalTime horaExame) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        TipoExame tipoExame = tipoExameRepository.findById(tipoExameId)
                .orElseThrow(() -> new RuntimeException("Tipo de exame não encontrado."));

        if (!tipoExame.getDisponivel()) {
            throw new RuntimeException("Este exame não está disponível no momento.");
        }

        UnidadeSaude unidade = unidadeSaudeRepository.findById(unidadeId)
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));

        if (!unidade.getAtiva()) {
            throw new RuntimeException("Unidade não está ativa.");
        }

        // Verifica se a data é futura
        if (dataExame.isBefore(LocalDate.now())) {
            throw new RuntimeException("A data do exame deve ser futura.");
        }

        // Verifica se o horário está disponível
        List<LocalTime> disponiveis = gerarHorariosDisponiveis(tipoExameId, dataExame);
        if (!disponiveis.contains(horaExame)) {
            throw new RuntimeException("Este horário não está mais disponível.");
        }

        // Verifica vagas no dia
        long agendadosNoDia = agendamentoRepository
                .countByTipoExameIdAndDataExameAndStatusNot(
                        tipoExameId, dataExame, StatusAgendamento.CANCELADO);

        if (agendadosNoDia >= tipoExame.getVagasPorDia()) {
            throw new RuntimeException("Não há mais vagas para este exame nesta data.");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setUsuario(usuario);
        agendamento.setTipoExame(tipoExame);
        agendamento.setUnidade(unidade);
        agendamento.setDataExame(dataExame);
        agendamento.setHoraExame(horaExame);
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> listarPorUsuario(Long usuarioId) {
        return agendamentoRepository.findByUsuarioIdOrderByDataExameDescHoraExameDesc(usuarioId);
    }

    public List<Agendamento> listarPorUnidade(Long unidadeId) {
        return agendamentoRepository.findByUnidadeIdOrderByDataExameDescHoraExameDesc(unidadeId);
    }

    public List<Agendamento> listarPorUnidadeEData(Long unidadeId, LocalDate data) {
        return agendamentoRepository.findByUnidadeIdAndDataExameOrderByHoraExameAsc(unidadeId, data);
    }

    public List<Agendamento> listarTodos() {
        return agendamentoRepository.findAll();
    }

    public Optional<Agendamento> buscarPorId(Long id) {
        return agendamentoRepository.findById(id);
    }

    public void cancelar(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new RuntimeException("Agendamento já foi realizado e não pode ser cancelado.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    public void atualizarStatus(Long id, StatusAgendamento status) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));

        agendamento.setStatus(status);
        agendamentoRepository.save(agendamento);
    }

    public long vagasDisponiveis(Long tipoExameId, LocalDate data) {
        TipoExame tipoExame = tipoExameRepository.findById(tipoExameId)
                .orElseThrow(() -> new RuntimeException("Tipo de exame não encontrado."));

        long agendados = agendamentoRepository
                .countByTipoExameIdAndDataExameAndStatusNot(
                        tipoExameId, data, StatusAgendamento.CANCELADO);

        return tipoExame.getVagasPorDia() - agendados;
    }

    public List<TipoExame> listarExamesPorUnidade(Long unidadeId) {
        return tipoExameRepository.findByUnidadeIdAndDisponivelTrue(unidadeId);
    }
}