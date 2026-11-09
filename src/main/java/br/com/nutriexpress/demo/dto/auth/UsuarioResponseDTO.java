package br.com.nutriexpress.demo.dto.auth;

import br.com.nutriexpress.demo.model.UserRole;
import br.com.nutriexpress.demo.model.Usuario;

public record UsuarioResponseDTO(
    Long id,
    String login,
    UserRole role,
    Boolean ativo
) {
    public UsuarioResponseDTO(Usuario usuario) {
        this(
            usuario.getId(),
            usuario.getLogin(),
            usuario.getRole(),
            usuario.getAtivo()
        );
    }
}
