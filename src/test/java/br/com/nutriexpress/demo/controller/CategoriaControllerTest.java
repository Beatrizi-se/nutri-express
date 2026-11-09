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

import br.com.nutriexpress.demo.dto.categoria.CategoriaRequestDTO;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.repository.UsuarioRepository;
import br.com.nutriexpress.demo.service.CategoriaService;
import br.com.nutriexpress.demo.service.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integracao da camada web para CategoriaController usando MockMvc.
 * Foco: Validar rotas HTTP, serializacao/desserializacao JSON, codigos de status e Bean Validation.
 */
@WebMvcTest(controllers = CategoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService categoriaService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("GET /api/v1/categorias - Deve retornar 200 OK com a lista de categorias")
    void listarCategorias_DeveRetornar200ComLista() throws Exception {
        Categoria cat1 = new Categoria(1L, "Pratos Saudaveis", "Refeicoes completas");

        when(categoriaService.listarCategorias()).thenReturn(List.of(cat1));

        mockMvc.perform(get("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Pratos Saudaveis"))
                .andExpect(jsonPath("$[0].descricao").value("Refeicoes completas"));
    }

    @Test
    @DisplayName("GET /api/v1/categorias/{id} - Deve retornar 200 OK quando ID existir")
    void getCategoriaById_QuandoExiste_DeveRetornar200() throws Exception {
        Categoria cat = new Categoria(2L, "Sucos Naturais", "Bebidas prensadas a frio");

        when(categoriaService.getCategoriaById(2L)).thenReturn(cat);

        mockMvc.perform(get("/api/v1/categorias/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Sucos Naturais"));
    }

    @Test
    @DisplayName("GET /api/v1/categorias/{id} - Deve retornar 404 Not Found quando ID nao existir")
    void getCategoriaById_QuandoNaoExiste_DeveRetornar404() throws Exception {
        when(categoriaService.getCategoriaById(99L))
                .thenThrow(new ResourceNotFoundException("Categoria não encontrada com o ID: 99"));

        mockMvc.perform(get("/api/v1/categorias/99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Categoria não encontrada com o ID: 99"));
    }

    @Test
    @DisplayName("POST /api/v1/categorias - Com dados validos deve retornar 201 Created")
    void criarCategoria_ComDadosValidos_DeveRetornar201() throws Exception {
        Categoria salva = new Categoria(3L, "Sobremesas Fit", "Doces sem acucar");

        when(categoriaService.criarCategoria(any(CategoriaRequestDTO.class))).thenReturn(salva);

        String jsonValido = """
            {
                "nome": "Sobremesas Fit",
                "descricao": "Doces sem acucar"
            }
            """;

        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.nome").value("Sobremesas Fit"));
    }

    @Test
    @DisplayName("POST /api/v1/categorias - Com nome em branco deve barrar no Bean Validation e retornar 400 Bad Request")
    void criarCategoria_ComNomeEmBranco_DeveRetornar400() throws Exception {
        String jsonInvalido = """
            {
                "nome": "",
                "descricao": "Descricao qualquer"
            }
            """;

        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.nome").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/categorias/{id} - Deve retornar 204 No Content")
    void deletarCategoria_DeveRetornar204() throws Exception {
        mockMvc.perform(delete("/api/v1/categorias/1"))
                .andExpect(status().isNoContent());
    }
}
