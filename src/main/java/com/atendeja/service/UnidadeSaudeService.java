package com.atendeja.service;

import com.atendeja.enums.TipoUnidade;
import com.atendeja.model.UnidadeSaude;
import com.atendeja.repository.UnidadeSaudeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UnidadeSaudeService {

    @Autowired
    private UnidadeSaudeRepository unidadeSaudeRepository;

    public List<UnidadeSaude> listarTodas() {
        return unidadeSaudeRepository.findAll();
    }

    public List<UnidadeSaude> listarAtivas() {
        return unidadeSaudeRepository.findByAtivaTrue();
    }

    public Optional<UnidadeSaude> buscarPorId(Long id) {
        return unidadeSaudeRepository.findById(id);
    }

    public UnidadeSaude salvar(String nome, String cnes, TipoUnidade tipo,
                               String endereco, String telefone,
                               String horario, Boolean ativa) {

        if (unidadeSaudeRepository.existsByCnes(cnes)) {
            throw new RuntimeException("CNES já cadastrado no sistema.");
        }

        UnidadeSaude unidade = new UnidadeSaude();
        unidade.setNome(nome);
        unidade.setCnes(cnes);
        unidade.setTipo(tipo);
        unidade.setEndereco(endereco);
        unidade.setTelefone(telefone);
        unidade.setHorario(horario);
        unidade.setAtiva(ativa != null ? ativa : true);

        return unidadeSaudeRepository.save(unidade);
    }

    public UnidadeSaude atualizar(Long id, String nome, TipoUnidade tipo,
                                  String endereco, String telefone,
                                  String horario, Boolean ativa) {

        UnidadeSaude unidade = unidadeSaudeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));

        unidade.setNome(nome);
        unidade.setTipo(tipo);
        unidade.setEndereco(endereco);
        unidade.setTelefone(telefone);
        unidade.setHorario(horario);
        unidade.setAtiva(ativa != null ? ativa : true);

        return unidadeSaudeRepository.save(unidade);
    }

    public void alternarStatus(Long id) {
        UnidadeSaude unidade = unidadeSaudeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));
        unidade.setAtiva(!unidade.getAtiva());
        unidadeSaudeRepository.save(unidade);
    }
}