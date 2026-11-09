package br.com.nutriexpress.demo.model;

/**
 * Papéis / Perfis de acesso dos usuários do sistema.
 */
public enum UserRole {
    ADMIN("admin"),
    CLIENTE("cliente");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
