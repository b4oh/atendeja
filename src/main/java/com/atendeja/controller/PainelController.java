package com.atendeja.controller;

import com.atendeja.service.GuicheService;
import com.atendeja.service.SenhaService;
import com.atendeja.service.SseService;
import com.atendeja.service.UnidadeSaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class PainelController {

    @Autowired
    private UnidadeSaudeService unidadeSaudeService;

    @Autowired
    private GuicheService guicheService;

    @Autowired
    private SenhaService senhaService;

    @Autowired
    private SseService sseService;

    // Selecionar unidade para o painel
    @GetMapping("/painel")
    public String selecionarUnidade(Model model) {
        model.addAttribute("unidades", unidadeSaudeService.listarAtivas());
        return "publico/painel-selecionar";
    }

    // Painel de chamada da unidade
    @GetMapping("/painel/{unidadeId}")
    public String painel(@PathVariable Long unidadeId, Model model) {
        model.addAttribute("unidade",
                unidadeSaudeService.buscarPorId(unidadeId).orElse(null));
        model.addAttribute("ultimasChamadas",
                guicheService.ultimasChamadas(unidadeId, 5));
        model.addAttribute("fila",
                senhaService.listarFilaAtual(unidadeId));
        return "publico/painel";
    }

    // Endpoint SSE — conexão persistente para atualização em tempo real
    @GetMapping(value = "/sse/painel/{unidadeId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter sseConexao(@PathVariable Long unidadeId) {
        return sseService.registrar(unidadeId);
    }
}