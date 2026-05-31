package com.atendeja.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    // Mapa de unidadeId → lista de conexões SSE abertas
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    // Registra uma nova conexão SSE para uma unidade
    public SseEmitter registrar(Long unidadeId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout infinito

        emitters.computeIfAbsent(unidadeId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        // Remove quando a conexão fechar
        emitter.onCompletion(() -> remover(unidadeId, emitter));
        emitter.onTimeout(() -> remover(unidadeId, emitter));
        emitter.onError(e -> remover(unidadeId, emitter));

        return emitter;
    }

    // Envia evento para todos os painéis conectados a uma unidade
    public void enviarParaPainel(Long unidadeId, Object dados) {
        CopyOnWriteArrayList<SseEmitter> lista = emitters.get(unidadeId);
        if (lista == null) return;

        for (SseEmitter emitter : lista) {
            try {
                emitter.send(SseEmitter.event()
                        .name("chamada")
                        .data(dados));
            } catch (IOException e) {
                remover(unidadeId, emitter);
            }
        }
    }

    private void remover(Long unidadeId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> lista = emitters.get(unidadeId);
        if (lista != null) {
            lista.remove(emitter);
        }
    }
}