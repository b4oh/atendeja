package com.atendeja.controller;

import com.atendeja.model.Usuario;
import com.atendeja.repository.UsuarioRepository;
import com.atendeja.service.RelatorioService;
import com.atendeja.service.UnidadeSaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/gestor")
public class GestorController {

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private UnidadeSaudeService unidadeSaudeService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);

        // Se é GESTOR com unidade, vai direto pro dashboard da unidade
        if (usuario != null && usuario.getUnidade() != null) {
            return "redirect:/gestor/dashboard/" + usuario.getUnidade().getId();
        }

        // Se é ADMIN, mostra seleção de unidades
        model.addAttribute("unidades", unidadeSaudeService.listarAtivas());
        return "gestor/selecionar-unidade";
    }

    @GetMapping("/dashboard/{unidadeId}")
    public String dashboard(@PathVariable Long unidadeId, Model model) {
        model.addAttribute("unidade",
                unidadeSaudeService.buscarPorId(unidadeId).orElse(null));

        // Cards principais
        model.addAttribute("senhasEmitidas",
                relatorioService.senhasEmitidasHoje(unidadeId));
        model.addAttribute("atendimentos",
                relatorioService.atendimentosHoje(unidadeId));
        model.addAttribute("naoComparecimentos",
                relatorioService.naoComparecimentosHoje(unidadeId));
        model.addAttribute("tempoMedio",
                relatorioService.tempoMedioEsperaHoje(unidadeId));
        model.addAttribute("naFila",
                relatorioService.senhasNaFila(unidadeId));
        model.addAttribute("taxaAtendimento",
                relatorioService.taxaAtendimento(unidadeId));

        // Gráficos
        model.addAttribute("porServico",
                relatorioService.atendimentosPorServico(unidadeId));
        model.addAttribute("porHora",
                relatorioService.atendimentosPorHora(unidadeId));

        // Últimos atendimentos
        model.addAttribute("ultimosAtendimentos",
                relatorioService.ultimosAtendimentos(unidadeId, 10));

        return "gestor/dashboard";
    }
}