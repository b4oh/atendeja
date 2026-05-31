package com.atendeja.controller;

import com.atendeja.enums.DesfechoAtendimento;
import com.atendeja.enums.StatusGuiche;
import com.atendeja.enums.StatusSenha;
import com.atendeja.model.Atendimento;
import com.atendeja.model.Guiche;
import com.atendeja.model.Senha;
import com.atendeja.model.Usuario;
import com.atendeja.repository.AtendimentoRepository;
import com.atendeja.repository.GuicheRepository;
import com.atendeja.repository.SenhaRepository;
import com.atendeja.repository.UsuarioRepository;
import com.atendeja.service.GuicheService;
import com.atendeja.service.SenhaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/atendente")
public class AtendenteController {

    @Autowired
    private GuicheService guicheService;

    @Autowired
    private SenhaService senhaService;

    @Autowired
    private GuicheRepository guicheRepository;

    @Autowired
    private SenhaRepository senhaRepository;

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Selecionar guichê
    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        if (usuario == null || usuario.getUnidade() == null) {
            model.addAttribute("erro", "Usuário sem unidade vinculada.");
            return "atendente/sem-unidade";
        }

        List<Guiche> guiches = guicheService.listarPorUnidade(usuario.getUnidade().getId());
        model.addAttribute("guiches", guiches);
        model.addAttribute("unidade", usuario.getUnidade());
        return "atendente/selecionar-guiche";
    }

    // Abrir guichê
    @PostMapping("/abrir/{guicheId}")
    public String abrirGuiche(@PathVariable Long guicheId, Authentication auth,
                              RedirectAttributes redirect) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        Guiche guiche = guicheRepository.findById(guicheId).orElse(null);

        if (guiche == null || usuario == null) {
            redirect.addFlashAttribute("erro", "Guichê ou usuário não encontrado.");
            return "redirect:/atendente/home";
        }

        guiche.setOperador(usuario);
        guiche.setStatus(StatusGuiche.DISPONIVEL);
        guicheRepository.save(guiche);

        return "redirect:/atendente/painel/" + guicheId;
    }

    // Painel do atendente
    @GetMapping("/painel/{guicheId}")
    public String painel(@PathVariable Long guicheId, Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        Guiche guiche = guicheRepository.findById(guicheId).orElse(null);

        if (guiche == null || usuario == null) {
            return "redirect:/atendente/home";
        }

        Long unidadeId = guiche.getUnidade().getId();

        // Senha sendo atendida atualmente (CHAMADA ou EM_ATENDIMENTO)
        Optional<Senha> senhaAtual = senhaRepository.findAll().stream()
                .filter(s -> s.getUnidade().getId().equals(unidadeId))
                .filter(s -> s.getStatus() == StatusSenha.CHAMADA
                        || s.getStatus() == StatusSenha.EM_ATENDIMENTO)
                .filter(s -> s.getChamadaEm() != null)
                .sorted((a, b) -> b.getChamadaEm().compareTo(a.getChamadaEm()))
                .findFirst();

        // Fila de senhas aguardando
        List<Senha> fila = senhaService.listarFilaAtual(unidadeId);

        model.addAttribute("usuario", usuario);
        model.addAttribute("guiche", guiche);
        model.addAttribute("unidade", guiche.getUnidade());
        model.addAttribute("senhaAtual", senhaAtual.orElse(null));
        model.addAttribute("fila", fila);
        model.addAttribute("desfechos", DesfechoAtendimento.values());

        return "atendente/painel";
    }

    // Chamar próxima senha
    @PostMapping("/chamar/{guicheId}")
    public String chamar(@PathVariable Long guicheId, RedirectAttributes redirect) {
        try {
            Senha senha = guicheService.chamarProxima(guicheId);
            redirect.addFlashAttribute("sucesso", "Senha " + senha.getNumero() + " chamada!");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/atendente/painel/" + guicheId;
    }

    // Registrar atendimento
    @PostMapping("/finalizar/{guicheId}")
    public String finalizar(@PathVariable Long guicheId,
                            @RequestParam Long senhaId,
                            @RequestParam DesfechoAtendimento desfecho,
                            @RequestParam(required = false) String cpfPaciente,
                            @RequestParam(required = false) String observacoes,
                            Authentication auth,
                            RedirectAttributes redirect) {

        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        Guiche guiche = guicheRepository.findById(guicheId).orElse(null);
        Senha senha = senhaRepository.findById(senhaId).orElse(null);

        if (senha == null || guiche == null || usuario == null) {
            redirect.addFlashAttribute("erro", "Dados não encontrados.");
            return "redirect:/atendente/painel/" + guicheId;
        }

        // Atualiza status da senha
        senha.setStatus(StatusSenha.ATENDIDA);
        senhaRepository.save(senha);

        // Cria registro de atendimento
        Atendimento atendimento = new Atendimento();
        atendimento.setSenha(senha);
        atendimento.setGuiche(guiche);
        atendimento.setAtendente(usuario);
        atendimento.setDesfecho(desfecho);
        atendimento.setCpfPaciente(cpfPaciente);
        atendimento.setObservacoes(observacoes);

        // Calcula tempo de espera
        if (senha.getEmitidaEm() != null && senha.getChamadaEm() != null) {
            long minutos = java.time.Duration.between(
                    senha.getEmitidaEm(), senha.getChamadaEm()).toMinutes();
            atendimento.setTempoEsperaMin((int) minutos);
        }

        atendimentoRepository.save(atendimento);

        // Libera guichê
        guiche.setStatus(StatusGuiche.DISPONIVEL);
        guicheRepository.save(guiche);

        redirect.addFlashAttribute("sucesso", "Atendimento registrado com sucesso!");
        return "redirect:/atendente/painel/" + guicheId;
    }

    // Fechar guichê
    @PostMapping("/fechar/{guicheId}")
    public String fechar(@PathVariable Long guicheId) {
        Guiche guiche = guicheRepository.findById(guicheId).orElse(null);
        if (guiche != null) {
            guiche.setStatus(StatusGuiche.FECHADO);
            guiche.setOperador(null);
            guicheRepository.save(guiche);
        }
        return "redirect:/atendente/home";
    }
}