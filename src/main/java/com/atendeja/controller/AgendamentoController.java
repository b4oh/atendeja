package com.atendeja.controller;

import com.atendeja.model.Agendamento;
import com.atendeja.model.Usuario;
import com.atendeja.repository.UsuarioRepository;
import com.atendeja.service.AgendamentoService;
import com.atendeja.service.UnidadeSaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/agendamento")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private UnidadeSaudeService unidadeSaudeService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Meus agendamentos
    @GetMapping
    public String meusAgendamentos(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";

        model.addAttribute("agendamentos", agendamentoService.listarPorUsuario(usuario.getId()));
        return "agendamento/meus-agendamentos";
    }

    // Selecionar unidade
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("unidades", unidadeSaudeService.listarAtivas());
        return "agendamento/selecionar-unidade";
    }

    // Selecionar exame
    @GetMapping("/novo/{unidadeId}")
    public String selecionarExame(@PathVariable Long unidadeId, Model model) {
        model.addAttribute("unidade", unidadeSaudeService.buscarPorId(unidadeId).orElse(null));
        model.addAttribute("exames", agendamentoService.listarExamesPorUnidade(unidadeId));
        return "agendamento/selecionar-exame";
    }

    // Formulário — selecionar data primeiro
    @GetMapping("/novo/{unidadeId}/{exameId}")
    public String formulario(@PathVariable Long unidadeId,
                             @PathVariable Long exameId,
                             @RequestParam(required = false) String data,
                             Model model) {
        model.addAttribute("unidade", unidadeSaudeService.buscarPorId(unidadeId).orElse(null));
        model.addAttribute("exame", agendamentoService.listarExamesPorUnidade(unidadeId).stream()
                .filter(e -> e.getId().equals(exameId)).findFirst().orElse(null));
        model.addAttribute("dataMinima", LocalDate.now().plusDays(1));

        // Se a data foi selecionada, busca horários disponíveis
        if (data != null && !data.isBlank()) {
            LocalDate dataSelecionada = LocalDate.parse(data);
            List<LocalTime> horarios = agendamentoService.gerarHorariosDisponiveis(exameId, dataSelecionada);
            model.addAttribute("dataSelecionada", data);
            model.addAttribute("horarios", horarios);
        }

        return "agendamento/formulario";
    }

    // Confirmar agendamento
    @PostMapping("/confirmar")
    public String confirmar(@RequestParam Long unidadeId,
                            @RequestParam Long exameId,
                            @RequestParam String dataExame,
                            @RequestParam String horaExame,
                            Authentication auth,
                            RedirectAttributes redirect) {
        try {
            Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
            if (usuario == null) return "redirect:/login";

            Agendamento agendamento = agendamentoService.agendar(
                    usuario.getId(), exameId, unidadeId,
                    LocalDate.parse(dataExame),
                    LocalTime.parse(horaExame));

            redirect.addFlashAttribute("agendamento", agendamento);
            return "redirect:/agendamento/comprovante";
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/agendamento/novo/" + unidadeId + "/" + exameId + "?data=" + dataExame;
        }
    }

    // Comprovante
    @GetMapping("/comprovante")
    public String comprovante() {
        return "agendamento/comprovante";
    }

    // Cancelar agendamento
    @PostMapping("/cancelar/{id}")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            agendamentoService.cancelar(id);
            redirect.addFlashAttribute("sucesso", "Agendamento cancelado com sucesso.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/agendamento";
    }
}