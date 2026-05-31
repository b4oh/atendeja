package com.atendeja.service;

import com.atendeja.enums.StatusGuiche;
import com.atendeja.enums.StatusSenha;
import com.atendeja.model.Guiche;
import com.atendeja.model.Senha;
import com.atendeja.repository.GuicheRepository;
import com.atendeja.repository.SenhaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GuicheService {

    @Autowired
    private GuicheRepository guicheRepository;

    @Autowired
    private SenhaRepository senhaRepository;

    @Autowired
    private SseService sseService;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private SenhaService senhaService;

    public Optional<Guiche> buscarPorId(Long id) {
        return guicheRepository.findById(id);
    }

    public List<Guiche> listarPorUnidade(Long unidadeId) {
        return guicheRepository.findByUnidadeId(unidadeId);
    }

    public Senha chamarProxima(Long guicheId) {
        Guiche guiche = guicheRepository.findById(guicheId)
                .orElseThrow(() -> new RuntimeException("Guichê não encontrado."));

        Long unidadeId = guiche.getUnidade().getId();

        // Busca próxima senha (prioritárias primeiro, depois FIFO)
        List<Senha> fila = senhaRepository
                .findByUnidadeIdAndStatusOrderByPrioritariaDescEmitidaEmAsc(
                        unidadeId, StatusSenha.AGUARDANDO);

        if (fila.isEmpty()) {
            throw new RuntimeException("Não há senhas na fila.");
        }

        Senha senha = fila.get(0);
        senha.setStatus(StatusSenha.CHAMADA);
        senha.setChamadaEm(LocalDateTime.now());
        senhaRepository.save(senha);

        // Atualiza status do guichê
        guiche.setStatus(StatusGuiche.EM_ATENDIMENTO);
        guicheRepository.save(guiche);

        // Envia evento SSE para o painel da TV
        Map<String, String> evento = new HashMap<>();
        evento.put("numero", senha.getNumero());
        evento.put("guiche", guiche.getNumero());
        evento.put("servico", senha.getServico().getNome());
        evento.put("prioritaria", senha.getPrioritaria().toString());
        sseService.enviarParaPainel(unidadeId, evento);

        // 1. Notifica SMS: senha foi chamada
        notificacaoService.notificarChamada(senha, guiche.getNumero());

        // 2. Notifica SMS: próximas 3 senhas da fila
        List<Senha> filaRestante = senhaRepository
                .findByUnidadeIdAndStatusOrderByPrioritariaDescEmitidaEmAsc(
                        unidadeId, StatusSenha.AGUARDANDO);

        senhaService.enviarFilaAtualizada(unidadeId);

        for (int i = 0; i < filaRestante.size(); i++) {
            int posicao = i + 1;
            notificacaoService.notificarProximidade(filaRestante.get(i), posicao);
        }

        return senha;
    }

    public List<Senha> ultimasChamadas(Long unidadeId, int limite) {
        return senhaRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .filter(s -> s.getStatus() == StatusSenha.CHAMADA
                        || s.getStatus() == StatusSenha.EM_ATENDIMENTO
                        || s.getStatus() == StatusSenha.ATENDIDA)
                .filter(s -> s.getChamadaEm() != null)
                .sorted((a, b) -> b.getChamadaEm().compareTo(a.getChamadaEm()))
                .limit(limite)
                .toList();
    }
}
