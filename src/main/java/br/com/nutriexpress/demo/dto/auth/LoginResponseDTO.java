package br.com.nutriexpress.demo.dto.auth;

public record LoginResponseDTO(
    String token,
    String tipo,
    String login,
    String role
) {
    public LoginResponseDTO(String token, String login, String role) {
        this(token, "Bearer", login, role);
    }
}
