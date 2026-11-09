package br.com.nutriexpress.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.nutriexpress.demo.model.UserRole;
import br.com.nutriexpress.demo.model.Usuario;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "chave-secreta-de-teste-para-token-jwt-123456");
        usuario = new Usuario("joao.silva", "senhaHash123", UserRole.CLIENTE);
    }

    @Test
    @DisplayName("Deve gerar um token JWT nao nulo e nao vazio")
    void gerarToken_ComUsuarioValido_DeveRetornarToken() {
        String token = tokenService.gerarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.split("\\.").length == 3, "JWT deve possuir 3 partes separadas por ponto");
    }

    @Test
    @DisplayName("Deve validar o token e recuperar o login do usuario (subject)")
    void validarToken_ComTokenValido_DeveRetornarLogin() {
        String token = tokenService.gerarToken(usuario);

        String loginRecuperado = tokenService.validarToken(token);

        assertEquals("joao.silva", loginRecuperado);
    }

    @Test
    @DisplayName("Deve retornar null ao validar token invalido")
    void validarToken_ComTokenInvalido_DeveRetornarNull() {
        String login = tokenService.validarToken("token.totalmente.invalido");

        assertNull(login);
    }
}
