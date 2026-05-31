package com.atendeja.controller;

import com.atendeja.enums.PerfilUsuario;
import com.atendeja.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cadastro")
public class CadastroController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String formulario() {
        return "publico/cadastro";
    }

    @PostMapping
    public String cadastrar(@RequestParam String nome,
                            @RequestParam String cpf,
                            @RequestParam String email,
                            @RequestParam String senha,
                            RedirectAttributes redirect) {
        try {
            usuarioService.salvar(cpf, nome, email, senha,
                    PerfilUsuario.CIDADAO, null, true);
            redirect.addFlashAttribute("sucesso",
                    "Conta criada com sucesso! Faça login para agendar exames.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            redirect.addFlashAttribute("nome", nome);
            redirect.addFlashAttribute("cpf", cpf);
            redirect.addFlashAttribute("email", email);
            return "redirect:/cadastro";
        }
    }
}