package com.atendeja.controller;

import com.atendeja.enums.PerfilUsuario;
import com.atendeja.model.Usuario;
import com.atendeja.repository.UnidadeSaudeRepository;
import com.atendeja.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UnidadeSaudeRepository unidadeSaudeRepository;

    // Listar todos os usuários
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "admin/usuarios/lista";
    }

    // Formulário de novo usuário
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("perfis", PerfilUsuario.values());
        model.addAttribute("unidades", unidadeSaudeRepository.findByAtivaTrue());
        return "admin/usuarios/form";
    }

    // Salvar novo usuário
    @PostMapping("/salvar")
    public String salvar(@RequestParam String cpf,
                         @RequestParam String nome,
                         @RequestParam String email,
                         @RequestParam String senha,
                         @RequestParam PerfilUsuario perfil,
                         @RequestParam(required = false) Long unidadeId,
                         @RequestParam(required = false) Boolean ativo,
                         RedirectAttributes redirect) {
        try {
            usuarioService.salvar(cpf, nome, email, senha, perfil, unidadeId, ativo);
            redirect.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso!");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/admin/usuarios/novo";
        }
        return "redirect:/admin/usuarios";
    }

    // Formulário de edição
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Optional<Usuario> usuario = usuarioService.buscarPorId(id);
        if (usuario.isEmpty()) {
            return "redirect:/admin/usuarios";
        }
        model.addAttribute("usuario", usuario.get());
        model.addAttribute("perfis", PerfilUsuario.values());
        model.addAttribute("unidades", unidadeSaudeRepository.findByAtivaTrue());
        return "admin/usuarios/form";
    }

    // Atualizar usuário
    @PostMapping("/atualizar/{id}")
    public String atualizar(@PathVariable Long id,
                            @RequestParam String nome,
                            @RequestParam String email,
                            @RequestParam(required = false) String senha,
                            @RequestParam PerfilUsuario perfil,
                            @RequestParam(required = false) Long unidadeId,
                            @RequestParam(required = false) Boolean ativo,
                            RedirectAttributes redirect) {
        try {
            usuarioService.atualizar(id, nome, email, senha, perfil, unidadeId, ativo);
            redirect.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/admin/usuarios/editar/" + id;
        }
        return "redirect:/admin/usuarios";
    }

    // Ativar/Desativar usuário
    @PostMapping("/status/{id}")
    public String alternarStatus(@PathVariable Long id,
                                 RedirectAttributes redirect) {
        usuarioService.alternarStatus(id);
        redirect.addFlashAttribute("sucesso", "Status alterado com sucesso!");
        return "redirect:/admin/usuarios";
    }
}