package com.atendeja.controller;

import com.atendeja.enums.TipoUnidade;
import com.atendeja.model.UnidadeSaude;
import com.atendeja.service.UnidadeSaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/unidades")
public class UnidadeController {

    @Autowired
    private UnidadeSaudeService unidadeSaudeService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("unidades", unidadeSaudeService.listarTodas());
        return "admin/unidades/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("tipos", TipoUnidade.values());
        return "admin/unidades/form";
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam String nome,
                         @RequestParam String cnes,
                         @RequestParam TipoUnidade tipo,
                         @RequestParam String endereco,
                         @RequestParam(required = false) String telefone,
                         @RequestParam(required = false) String horario,
                         @RequestParam(required = false) Boolean ativa,
                         RedirectAttributes redirect) {
        try {
            unidadeSaudeService.salvar(nome, cnes, tipo, endereco, telefone, horario, ativa);
            redirect.addFlashAttribute("sucesso", "Unidade cadastrada com sucesso!");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/admin/unidades/nova";
        }
        return "redirect:/admin/unidades";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Optional<UnidadeSaude> unidade = unidadeSaudeService.buscarPorId(id);
        if (unidade.isEmpty()) {
            return "redirect:/admin/unidades";
        }
        model.addAttribute("unidade", unidade.get());
        model.addAttribute("tipos", TipoUnidade.values());
        return "admin/unidades/form";
    }

    @PostMapping("/atualizar/{id}")
    public String atualizar(@PathVariable Long id,
                            @RequestParam String nome,
                            @RequestParam TipoUnidade tipo,
                            @RequestParam String endereco,
                            @RequestParam(required = false) String telefone,
                            @RequestParam(required = false) String horario,
                            @RequestParam(required = false) Boolean ativa,
                            RedirectAttributes redirect) {
        try {
            unidadeSaudeService.atualizar(id, nome, tipo, endereco, telefone, horario, ativa);
            redirect.addFlashAttribute("sucesso", "Unidade atualizada com sucesso!");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/admin/unidades/editar/" + id;
        }
        return "redirect:/admin/unidades";
    }

    @PostMapping("/status/{id}")
    public String alternarStatus(@PathVariable Long id,
                                 RedirectAttributes redirect) {
        unidadeSaudeService.alternarStatus(id);
        redirect.addFlashAttribute("sucesso", "Status alterado com sucesso!");
        return "redirect:/admin/unidades";
    }
}