package com.atendeja.controller;

import com.atendeja.model.Agendamento;
import com.atendeja.service.AgendamentoService;
import com.atendeja.service.UnidadeSaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class TransparenciaController {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private UnidadeSaudeService unidadeSaudeService;

    @GetMapping("/transparencia")
    public String transparencia(
            @RequestParam(required = false) Long unidadeId,
            @RequestParam(required = false) String data,
            Model model) {

        model.addAttribute("unidades", unidadeSaudeService.listarAtivas());

        List<Agendamento> agendamentos;

        if (unidadeId != null && data != null && !data.isBlank()) {
            agendamentos = agendamentoService.listarPorUnidadeEData(
                    unidadeId, LocalDate.parse(data));
            model.addAttribute("unidadeSelecionada", unidadeId);
            model.addAttribute("dataSelecionada", data);
        } else if (unidadeId != null) {
            agendamentos = agendamentoService.listarPorUnidade(unidadeId);
            model.addAttribute("unidadeSelecionada", unidadeId);
        } else {
            agendamentos = agendamentoService.listarTodos();
        }

        model.addAttribute("agendamentos", agendamentos);
        return "publico/transparencia";
    }
}