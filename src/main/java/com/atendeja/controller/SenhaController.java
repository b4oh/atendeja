package com.atendeja.controller;

import com.atendeja.enums.CanalEmissao;
import com.atendeja.model.Senha;
import com.atendeja.service.SenhaService;
import com.atendeja.service.ServicoService;
import com.atendeja.service.UnidadeSaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SenhaController {

    @Autowired
    private SenhaService senhaService;

    @Autowired
    private UnidadeSaudeService unidadeSaudeService;

    @Autowired
    private ServicoService servicoService;

    // Tela do totem — selecionar unidade
    @GetMapping("/totem")
    public String totem(Model model) {
        model.addAttribute("unidades", unidadeSaudeService.listarAtivas());
        return "publico/totem";
    }

    // Tela do totem — selecionar serviço
    @GetMapping("/totem/{unidadeId}")
    public String totemServicos(@PathVariable Long unidadeId, Model model) {
        model.addAttribute("unidade", unidadeSaudeService.buscarPorId(unidadeId).orElse(null));
        model.addAttribute("servicos", servicoService.listarPorUnidade(unidadeId));
        return "publico/totem-servicos";
    }

    // Emitir senha
    @PostMapping("/totem/emitir")
    public String emitir(@RequestParam Long unidadeId,
                         @RequestParam Long servicoId,
                         @RequestParam(required = false) Boolean prioritaria,
                         @RequestParam(required = false) String celular,
                         RedirectAttributes redirect) {
        try {
            Senha senha = senhaService.emitirSenha(unidadeId, servicoId,
                    prioritaria, celular, CanalEmissao.TOTEM);

            Integer posicao = senhaService.getPosicaoNaFila(senha);
            Integer tempoEstimado = senhaService.getTempoEstimado(posicao);

            redirect.addFlashAttribute("senha", senha);
            redirect.addFlashAttribute("posicao", posicao);
            redirect.addFlashAttribute("tempoEstimado", tempoEstimado);
            return "redirect:/totem/comprovante";
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/totem/" + unidadeId;
        }
    }

    // Comprovante da senha
    @GetMapping("/totem/comprovante")
    public String comprovante() {
        return "publico/comprovante";
    }

    // Consulta pública da posição na fila
    @GetMapping("/senha/consultar")
    public String consultarForm() {
        return "publico/consultar";
    }

    @PostMapping("/senha/consultar")
    public String consultar(@RequestParam String numero,
                            @RequestParam Long unidadeId,
                            Model model) {
        model.addAttribute("unidades", unidadeSaudeService.listarAtivas());

        var senhaOpt = senhaService.listarFilaAtual(unidadeId).stream()
                .filter(s -> s.getNumero().equalsIgnoreCase(numero))
                .findFirst();

        if (senhaOpt.isPresent()) {
            Senha senha = senhaOpt.get();
            model.addAttribute("senha", senha);
            model.addAttribute("posicao", senhaService.getPosicaoNaFila(senha));
            model.addAttribute("tempoEstimado",
                    senhaService.getTempoEstimado(senhaService.getPosicaoNaFila(senha)));
        } else {
            model.addAttribute("naoEncontrada", true);
        }

        return "publico/consultar";
    }
}