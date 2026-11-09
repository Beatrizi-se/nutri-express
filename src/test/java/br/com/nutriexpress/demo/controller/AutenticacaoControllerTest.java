package br.com.nutriexpress.demo.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.nutriexpress.demo.model.UserRole;
import br.com.nutriexpress.demo.model.Usuario;
import br.com.nutriexpress.demo.repository.UsuarioRepository;
import br.com.nutriexpress.demo.service.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integracao da camada web para AutenticacaoController usando MockMvc.
 */
@WebMvcTest(controllers = AutenticacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutenticacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /auth/login - Com credenciais validas deve autenticar e retornar 200 OK com token JWT")
    void login_ComSucesso_DeveRetornarTokenJWT() throws Exception {
        Usuario usuario = new Usuario(1L, "admin", "hashSenha", UserRole.ADMIN, true);
        Authentication authMock = mock(Authentication.class);
        when(authMock.getPrincipal()).thenReturn(usuario);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);
        when(tokenService.gerarToken(usuario)).thenReturn("token.jwt.mockado");

        String json = """
            {
                "login": "admin",
                "senha": "admin123"
            }
            """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token.jwt.mockado"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.login").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /auth/login - Com login em branco deve retornar 400 Bad Request")
    void login_ComLoginEmBranco_DeveRetornar400() throws Exception {
        String json = """
            {
                "login": "",
                "senha": "123"
            }
            """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.login").exists());
    }

    @Test
    @DisplayName("POST /auth/registrar - Com dados validos deve salvar e retornar 201 Created")
    void registrar_ComSucesso_DeveRetornar201() throws Exception {
        when(usuarioRepository.existsByLogin("novo.usuario")).thenReturn(false);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("hashSeguro");

        Usuario salvo = new Usuario(2L, "novo.usuario", "hashSeguro", UserRole.CLIENTE, true);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(salvo);

        String json = """
            {
                "login": "novo.usuario",
                "senha": "senhaSegura123",
                "role": "CLIENTE"
            }
            """;

        mockMvc.perform(post("/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.login").value("novo.usuario"))
                .andExpect(jsonPath("$.role").value("CLIENTE"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("POST /auth/registrar - Com login ja existente deve retornar 409 Conflict")
    void registrar_ComLoginExistente_DeveRetornar409Conflict() throws Exception {
        when(usuarioRepository.existsByLogin("admin")).thenReturn(true);

        String json = """
            {
                "login": "admin",
                "senha": "senhaSegura123",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Já existe um usuário cadastrado com o login: admin"));
    }

    @Test
    @DisplayName("POST /auth/registrar - Com senha menor que 6 caracteres deve retornar 400 Bad Request")
    void registrar_ComSenhaCurta_DeveRetornar400() throws Exception {
        String json = """
            {
                "login": "usuario.teste",
                "senha": "123"
            }
            """;

        mockMvc.perform(post("/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.senha").exists());
    }
}
