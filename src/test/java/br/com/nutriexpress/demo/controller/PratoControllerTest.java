package br.com.nutriexpress.demo.controller;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.nutriexpress.demo.dto.prato.PratoRequestDTO;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.model.Prato;
import br.com.nutriexpress.demo.repository.UsuarioRepository;
import br.com.nutriexpress.demo.service.PratoService;
import br.com.nutriexpress.demo.service.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integracao da camada web para PratoController usando MockMvc.
 */
@WebMvcTest(controllers = PratoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PratoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PratoService pratoService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Prato criarPratoExemplo() {
        Categoria categoria = new Categoria(1L, "Saladas", "Pratos leves e saladas");
        return new Prato(10L, "Salada Caesar Fit", "Alface, frango e molho leve", 28.50, 220, true, categoria, 15);
    }

    @Test
    @DisplayName("GET /api/v1/pratos - Deve retornar 200 OK com lista de pratos")
    void listarPratos_DeveRetornar200ComLista() throws Exception {
        when(pratoService.listarTodos()).thenReturn(List.of(criarPratoExemplo()));

        mockMvc.perform(get("/api/v1/pratos")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Salada Caesar Fit"))
                .andExpect(jsonPath("$[0].preco").value(28.50))
                .andExpect(jsonPath("$[0].quantidadeEstoque").value(15));
    }

    @Test
    @DisplayName("GET /api/v1/pratos/{id} - Deve retornar 200 OK quando ID existir")
    void buscarPorId_QuandoExiste_DeveRetornar200() throws Exception {
        when(pratoService.buscarPorId(10L)).thenReturn(criarPratoExemplo());

        mockMvc.perform(get("/api/v1/pratos/10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Salada Caesar Fit"));
    }

    @Test
    @DisplayName("GET /api/v1/pratos/{id} - Deve retornar 404 Not Found quando ID nao existir")
    void buscarPorId_QuandoNaoExiste_DeveRetornar404() throws Exception {
        when(pratoService.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Prato não encontrado com o ID: 99"));

        mockMvc.perform(get("/api/v1/pratos/99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Prato não encontrado com o ID: 99"));
    }

    @Test
    @DisplayName("POST /api/v1/pratos - Com dados validos deve cadastrar e retornar 201 Created")
    void criarPrato_ComDadosValidos_DeveRetornar201() throws Exception {
        when(pratoService.criarPrato(any(PratoRequestDTO.class))).thenReturn(criarPratoExemplo());

        String jsonValido = """
            {
                "nome": "Salada Caesar Fit",
                "descricao": "Alface, frango e molho leve",
                "preco": 28.50,
                "calorias": 220,
                "disponivel": true,
                "categoriaId": 1,
                "quantidadeEstoque": 15
            }
            """;

        mockMvc.perform(post("/api/v1/pratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Salada Caesar Fit"))
                .andExpect(jsonPath("$.preco").value(28.50));
    }

    @Test
    @DisplayName("POST /api/v1/pratos - Com preco negativo deve retornar 400 Bad Request")
    void criarPrato_ComPrecoNegativo_DeveRetornar400() throws Exception {
        String jsonInvalido = """
            {
                "nome": "Salada Fit",
                "descricao": "Descricao",
                "preco": -5.0,
                "calorias": 200,
                "disponivel": true,
                "categoriaId": 1,
                "quantidadeEstoque": 10
            }
            """;

        mockMvc.perform(post("/api/v1/pratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.preco").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/pratos/{id}/preco - Deve atualizar preco e retornar 200 OK")
    void atualizarPreco_DeveRetornar200() throws Exception {
        Prato atualizado = criarPratoExemplo();
        atualizado.setPreco(32.0);

        when(pratoService.atualizarPreco(eq(10L), eq(32.0))).thenReturn(atualizado);

        mockMvc.perform(patch("/api/v1/pratos/10/preco")
                .param("preco", "32.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(32.0));
    }

    @Test
    @DisplayName("PATCH /api/v1/pratos/{id}/estoque - Deve atualizar estoque e retornar 200 OK")
    void atualizarEstoque_DeveRetornar200() throws Exception {
        Prato atualizado = criarPratoExemplo();
        atualizado.setQuantidadeEstoque(50);

        when(pratoService.atualizarEstoque(eq(10L), eq(50))).thenReturn(atualizado);

        mockMvc.perform(patch("/api/v1/pratos/10/estoque")
                .param("quantidade", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(50));
    }

    @Test
    @DisplayName("DELETE /api/v1/pratos/{id} - Deve excluir e retornar 204 No Content")
    void deletarPrato_DeveRetornar204() throws Exception {
        doNothing().when(pratoService).deletarPrato(10L);

        mockMvc.perform(delete("/api/v1/pratos/10"))
                .andExpect(status().isNoContent());
    }
}
