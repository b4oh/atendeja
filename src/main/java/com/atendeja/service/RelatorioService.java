package com.atendeja.service;

import com.atendeja.enums.StatusSenha;
import com.atendeja.model.Atendimento;
import com.atendeja.model.Senha;
import com.atendeja.repository.AtendimentoRepository;
import com.atendeja.repository.SenhaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    @Autowired
    private SenhaRepository senhaRepository;

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    // Senhas emitidas hoje por unidade
    public long senhasEmitidasHoje(Long unidadeId) {
        return senhaRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .filter(s -> s.getEmitidaEm().toLocalDate().equals(LocalDate.now()))
                .count();
    }

    // Atendimentos realizados hoje por unidade
    public long atendimentosHoje(Long unidadeId) {
        return atendimentoRepository.findAll().stream()
                .filter(a -> a.getGuiche().getUnidade().getId().equals(unidadeId))
                .filter(a -> a.getRealizadoEm().toLocalDate().equals(LocalDate.now()))
                .count();
    }

    // Não comparecimentos hoje
    public long naoComparecimentosHoje(Long unidadeId) {
        return atendimentoRepository.findAll().stream()
                .filter(a -> a.getGuiche().getUnidade().getId().equals(unidadeId))
                .filter(a -> a.getRealizadoEm().toLocalDate().equals(LocalDate.now()))
                .filter(a -> a.getDesfecho().name().equals("NAO_COMPARECEU"))
                .count();
    }

    // Tempo médio de espera hoje (minutos)
    public double tempoMedioEsperaHoje(Long unidadeId) {
        List<Atendimento> atendimentos = atendimentoRepository.findAll().stream()
                .filter(a -> a.getGuiche().getUnidade().getId().equals(unidadeId))
                .filter(a -> a.getRealizadoEm().toLocalDate().equals(LocalDate.now()))
                .filter(a -> a.getTempoEsperaMin() != null && a.getTempoEsperaMin() > 0)
                .toList();

        if (atendimentos.isEmpty()) return 0;

        return atendimentos.stream()
                .mapToInt(Atendimento::getTempoEsperaMin)
                .average()
                .orElse(0);
    }

    // Senhas na fila agora
    public long senhasNaFila(Long unidadeId) {
        return senhaRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .filter(s -> s.getStatus() == StatusSenha.AGUARDANDO)
                .filter(s -> s.getEmitidaEm().toLocalDate().equals(LocalDate.now()))
                .count();
    }

    // Atendimentos por serviço hoje
    public Map<String, Long> atendimentosPorServico(Long unidadeId) {
        return senhaRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .filter(s -> s.getEmitidaEm().toLocalDate().equals(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        s -> s.getServico().getNome(),
                        Collectors.counting()
                ));
    }

    // Atendimentos por hora hoje
    public Map<Integer, Long> atendimentosPorHora(Long unidadeId) {
        Map<Integer, Long> porHora = senhaRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .filter(s -> s.getEmitidaEm().toLocalDate().equals(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        s -> s.getEmitidaEm().getHour(),
                        TreeMap::new,
                        Collectors.counting()
                ));
        return porHora;
    }

    // Últimos atendimentos do dia
    public List<Atendimento> ultimosAtendimentos(Long unidadeId, int limite) {
        return atendimentoRepository.findAll().stream()
                .filter(a -> a.getGuiche().getUnidade().getId().equals(unidadeId))
                .filter(a -> a.getRealizadoEm().toLocalDate().equals(LocalDate.now()))
                .sorted((a, b) -> b.getRealizadoEm().compareTo(a.getRealizadoEm()))
                .limit(limite)
                .toList();
    }

    // Taxa de atendimento (atendidos / emitidas * 100)
    public double taxaAtendimento(Long unidadeId) {
        long emitidas = senhasEmitidasHoje(unidadeId);
        long atendidos = atendimentosHoje(unidadeId);
        if (emitidas == 0) return 0;
        return Math.round((double) atendidos / emitidas * 1000.0) / 10.0;
    }
}