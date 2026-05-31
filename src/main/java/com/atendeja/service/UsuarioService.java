package com.atendeja.service;

import com.atendeja.service.CpfValidator;
import com.atendeja.enums.PerfilUsuario;
import com.atendeja.model.UnidadeSaude;
import com.atendeja.model.Usuario;
import com.atendeja.repository.UnidadeSaudeRepository;
import com.atendeja.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private CpfValidator cpfValidator;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadeSaudeRepository unidadeSaudeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Usuario salvar(String cpf, String nome, String email,
                          String senhaTexto, PerfilUsuario perfil,
                          Long unidadeId, Boolean ativo) {
        // Valida CPF
        if (!cpfValidator.isValido(cpf)) {
            throw new RuntimeException("CPF inválido. Verifique os números digitados.");
        }

        // Verifica duplicidade de CPF
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new RuntimeException("CPF já cadastrado no sistema.");
        }

        // Verifica duplicidade de email
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("E-mail já cadastrado no sistema.");
        }

        Usuario usuario = new Usuario();
        usuario.setCpf(cpf);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senhaTexto));
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo != null ? ativo : true);

        // Vincula unidade se informada
        if (unidadeId != null) {
            UnidadeSaude unidade = unidadeSaudeRepository.findById(unidadeId)
                    .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));
            usuario.setUnidade(unidade);
        }

        return usuarioRepository.save(usuario);
    }

    public Usuario atualizar(Long id, String nome, String email,
                             String senhaTexto, PerfilUsuario perfil,
                             Long unidadeId, Boolean ativo) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Verifica se email mudou e se já existe
        if (!usuario.getEmail().equals(email) &&
                usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("E-mail já cadastrado no sistema.");
        }

        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo != null ? ativo : true);

        // Só atualiza senha se uma nova foi informada
        if (senhaTexto != null && !senhaTexto.isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(senhaTexto));
        }

        // Atualiza unidade
        if (unidadeId != null) {
            UnidadeSaude unidade = unidadeSaudeRepository.findById(unidadeId)
                    .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));
            usuario.setUnidade(unidade);
        } else {
            usuario.setUnidade(null);
        }

        return usuarioRepository.save(usuario);
    }

    public void alternarStatus(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        usuario.setAtivo(!usuario.getAtivo());
        usuarioRepository.save(usuario);
    }
}