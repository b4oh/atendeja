package com.atendeja.service;

import com.atendeja.model.Servico;
import com.atendeja.model.UnidadeSaude;
import com.atendeja.repository.ServicoRepository;
import com.atendeja.repository.UnidadeSaudeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServicoService {

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private UnidadeSaudeRepository unidadeSaudeRepository;

    public List<Servico> listarPorUnidade(Long unidadeId) {
        return servicoRepository.findByUnidadeIdAndDisponivelTrue(unidadeId);
    }

    public List<Servico> listarTodosPorUnidade(Long unidadeId) {
        return servicoRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .toList();
    }

    public Optional<Servico> buscarPorId(Long id) {
        return servicoRepository.findById(id);
    }

    public Servico salvar(Long unidadeId, String nome, Character prefixo,
                          Integer capacidadeDiaria, Boolean disponivel) {

        if (servicoRepository.existsByUnidadeIdAndPrefixo(unidadeId, prefixo)) {
            throw new RuntimeException("Prefixo '" + prefixo + "' já está em uso nesta unidade.");
        }

        UnidadeSaude unidade = unidadeSaudeRepository.findById(unidadeId)
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));

        Servico servico = new Servico();
        servico.setUnidade(unidade);
        servico.setNome(nome);
        servico.setPrefixo(prefixo);
        servico.setCapacidadeDiaria(capacidadeDiaria != null ? capacidadeDiaria : 50);
        servico.setDisponivel(disponivel != null ? disponivel : true);

        return servicoRepository.save(servico);
    }

    public Servico atualizar(Long id, String nome, Integer capacidadeDiaria,
                             Boolean disponivel) {

        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado."));

        servico.setNome(nome);
        servico.setCapacidadeDiaria(capacidadeDiaria != null ? capacidadeDiaria : 50);
        servico.setDisponivel(disponivel != null ? disponivel : true);

        return servicoRepository.save(servico);
    }

    public void alternarDisponibilidade(Long id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado."));
        servico.setDisponivel(!servico.getDisponivel());
        servicoRepository.save(servico);
    }
}