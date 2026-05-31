package com.atendeja.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    // Mapa de unidadeId para conexoes SSE abertas.
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    // Registra uma nova conexao SSE para uma unidade.
    public SseEmitter registrar(Long unidadeId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(unidadeId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> remover(unidadeId, emitter));
        emitter.onTimeout(() -> remover(unidadeId, emitter));
        emitter.onError(e -> remover(unidadeId, emitter));

        return emitter;
    }

    // Envia evento para todos os clientes conectados a uma unidade.
    public void enviarEvento(Long unidadeId, String nomeEvento, Object dados) {
        CopyOnWriteArrayList<SseEmitter> lista = emitters.get(unidadeId);
        if (lista == null) return;

        for (SseEmitter emitter : lista) {
            try {
                emitter.send(SseEmitter.event()
                        .name(nomeEvento)
                        .data(dados));
            } catch (IOException e) {
                remover(unidadeId, emitter);
            }
        }
    }

    // Evento usado pelo painel de TV quando uma senha e chamada.
    public void enviarParaPainel(Long unidadeId, Object dados) {
        enviarEvento(unidadeId, "chamada", dados);
    }

    // Evento usado pelo painel do atendente para atualizar a fila lateral.
    public void enviarFilaAtualizada(Long unidadeId, Object dados) {
        enviarEvento(unidadeId, "filaAtualizada", dados);
    }

    private void remover(Long unidadeId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> lista = emitters.get(unidadeId);
        if (lista != null) {
            lista.remove(emitter);
        }
    }
}
