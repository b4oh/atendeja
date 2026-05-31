package com.atendeja.service;

import com.atendeja.enums.CanalEmissao;
import com.atendeja.enums.StatusSenha;
import com.atendeja.model.Senha;
import com.atendeja.model.Servico;
import com.atendeja.model.UnidadeSaude;
import com.atendeja.repository.SenhaRepository;
import com.atendeja.repository.ServicoRepository;
import com.atendeja.repository.UnidadeSaudeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SenhaService {

    @Autowired
    private SenhaRepository senhaRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private UnidadeSaudeRepository unidadeSaudeRepository;

    public Senha emitirSenha(Long unidadeId, Long servicoId,
                             Boolean prioritaria, String celular,
                             CanalEmissao canal) {

        UnidadeSaude unidade = unidadeSaudeRepository.findById(unidadeId)
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));

        if (!unidade.getAtiva()) {
            throw new RuntimeException("Unidade não está ativa.");
        }

        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado."));

        if (!servico.getDisponivel()) {
            throw new RuntimeException("Serviço indisponível no momento.");
        }

        // Verifica capacidade diária
        Integer senhasHoje = senhaRepository.countSenhasHojePorServico(servicoId);
        if (senhasHoje >= servico.getCapacidadeDiaria()) {
            throw new RuntimeException("Capacidade diária atingida para este serviço.");
        }

        // Gera o número da senha (prefixo + sequencial do dia)
        String numero = gerarNumero(servico.getPrefixo(), unidadeId);

        Senha senha = new Senha();
        senha.setUnidade(unidade);
        senha.setServico(servico);
        senha.setNumero(numero);
        senha.setPrioritaria(prioritaria != null ? prioritaria : false);
        senha.setCanalEmissao(canal != null ? canal : CanalEmissao.PORTAL_WEB);
        senha.setCelular(celular);
        senha.setStatus(StatusSenha.AGUARDANDO);

        return senhaRepository.save(senha);
    }

    private String gerarNumero(Character prefixo, Long unidadeId) {
        // Conta senhas do dia para esse prefixo
        List<Senha> senhasDoDia = senhaRepository
                .findByUnidadeIdAndStatusOrderByPrioritariaDescEmitidaEmAsc(
                        unidadeId, StatusSenha.AGUARDANDO);

        // Busca todas as senhas do dia para contar o sequencial
        long count = senhaRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .filter(s -> s.getNumero().startsWith(prefixo.toString()))
                .filter(s -> s.getEmitidaEm().toLocalDate().equals(LocalDate.now()))
                .count();

        int sequencial = (int) count + 1;
        return prefixo + String.format("%03d", sequencial);
    }

    public List<Senha> listarFilaAtual(Long unidadeId) {
        return senhaRepository
                .findByUnidadeIdAndStatusOrderByPrioritariaDescEmitidaEmAsc(
                        unidadeId, StatusSenha.AGUARDANDO);
    }

    public Integer getPosicaoNaFila(Senha senha) {
        List<Senha> fila = listarFilaAtual(senha.getUnidade().getId());
        for (int i = 0; i < fila.size(); i++) {
            if (fila.get(i).getId().equals(senha.getId())) {
                return i + 1;
            }
        }
        return 0;
    }

    public Integer getTempoEstimado(Integer posicao) {
        // Tempo médio por atendimento: ~6 minutos
        return posicao * 6;
    }
}