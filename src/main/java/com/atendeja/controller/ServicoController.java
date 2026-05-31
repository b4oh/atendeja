package com.atendeja.controller;

import com.atendeja.model.UnidadeSaude;
import com.atendeja.service.ServicoService;
import com.atendeja.service.UnidadeSaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/unidades/{unidadeId}/servicos")
public class ServicoController {

    @Autowired
    private ServicoService servicoService;

    @Autowired
    private UnidadeSaudeService unidadeSaudeService;

    @GetMapping
    public String listar(@PathVariable Long unidadeId, Model model) {
        Optional<UnidadeSaude> unidade = unidadeSaudeService.buscarPorId(unidadeId);
        if (unidade.isEmpty()) {
            return "redirect:/admin/unidades";
        }
        model.addAttribute("unidade", unidade.get());
        model.addAttribute("servicos", servicoService.listarTodosPorUnidade(unidadeId));
        return "admin/servicos/lista";
    }

    @GetMapping("/novo")
    public String novo(@PathVariable Long unidadeId, Model model) {
        Optional<UnidadeSaude> unidade = unidadeSaudeService.buscarPorId(unidadeId);
        if (unidade.isEmpty()) {
            return "redirect:/admin/unidades";
        }
        model.addAttribute("unidade", unidade.get());
        return "admin/servicos/form";
    }

    @PostMapping("/salvar")
    public String salvar(@PathVariable Long unidadeId,
                         @RequestParam String nome,
                         @RequestParam Character prefixo,
                         @RequestParam(required = false) Integer capacidadeDiaria,
                         @RequestParam(required = false) Boolean disponivel,
                         RedirectAttributes redirect) {
        try {
            servicoService.salvar(unidadeId, nome, prefixo, capacidadeDiaria, disponivel);
            redirect.addFlashAttribute("sucesso", "Serviço cadastrado com sucesso!");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/admin/unidades/" + unidadeId + "/servicos/novo";
        }
        return "redirect:/admin/unidades/" + unidadeId + "/servicos";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long unidadeId,
                         @PathVariable Long id, Model model) {
        Optional<UnidadeSaude> unidade = unidadeSaudeService.buscarPorId(unidadeId);
        if (unidade.isEmpty()) {
            return "redirect:/admin/unidades";
        }
        model.addAttribute("unidade", unidade.get());
        model.addAttribute("servico", servicoService.buscarPorId(id).orElse(null));
        return "admin/servicos/form";
    }

    @PostMapping("/atualizar/{id}")
    public String atualizar(@PathVariable Long unidadeId,
                            @PathVariable Long id,
                            @RequestParam String nome,
                            @RequestParam(required = false) Integer capacidadeDiaria,
                            @RequestParam(required = false) Boolean disponivel,
                            RedirectAttributes redirect) {
        try {
            servicoService.atualizar(id, nome, capacidadeDiaria, disponivel);
            redirect.addFlashAttribute("sucesso", "Serviço atualizado com sucesso!");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/unidades/" + unidadeId + "/servicos";
    }

    @PostMapping("/status/{id}")
    public String alternarStatus(@PathVariable Long unidadeId,
                                 @PathVariable Long id,
                                 RedirectAttributes redirect) {
        servicoService.alternarDisponibilidade(id);
        redirect.addFlashAttribute("sucesso", "Disponibilidade alterada!");
        return "redirect:/admin/unidades/" + unidadeId + "/servicos";
    }
}