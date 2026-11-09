package br.com.nutriexpress.demo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.nutriexpress.demo.dto.pedido.PedidoRequestDTO;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.ItemPedido;
import br.com.nutriexpress.demo.model.Pedido;
import br.com.nutriexpress.demo.model.StatusPedido;
import br.com.nutriexpress.demo.repository.UsuarioRepository;
import br.com.nutriexpress.demo.service.PedidoService;
import br.com.nutriexpress.demo.service.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integracao da camada web para PedidoController usando MockMvc.
 */
@WebMvcTest(controllers = PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Pedido criarPedidoExemplo() {
        Pedido pedido = new Pedido();
        pedido.setId(100L);
        pedido.setClienteId(1L);
        pedido.setClienteNome("Maria Silva");
        pedido.setDataHora(LocalDateTime.now());
        pedido.setStatus(StatusPedido.PENDENTE);

        ItemPedido item = new ItemPedido(10L, "Salada Caesar", 2, 25.0);
        pedido.adicionarItem(item);

        return pedido;
    }

    @Test
    @DisplayName("GET /api/v1/pedidos - Deve retornar 200 OK com lista de pedidos")
    void listarPedidos_DeveRetornar200ComLista() throws Exception {
        when(pedidoService.listarTodos()).thenReturn(List.of(criarPedidoExemplo()));

        mockMvc.perform(get("/api/v1/pedidos")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].clienteId").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[0].valorTotal").value(50.0));
    }

    @Test
    @DisplayName("GET /api/v1/pedidos/{id} - Deve retornar 200 OK quando ID existir")
    void buscarPorId_QuandoExiste_DeveRetornar200() throws Exception {
        when(pedidoService.buscarPorId(100L)).thenReturn(criarPedidoExemplo());

        mockMvc.perform(get("/api/v1/pedidos/100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.valorTotal").value(50.0));
    }

    @Test
    @DisplayName("GET /api/v1/pedidos/{id} - Deve retornar 404 Not Found quando ID nao existir")
    void buscarPorId_QuandoNaoExiste_DeveRetornar404() throws Exception {
        when(pedidoService.buscarPorId(999L))
                .thenThrow(new ResourceNotFoundException("Pedido não encontrado com o ID: 999"));

        mockMvc.perform(get("/api/v1/pedidos/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pedido não encontrado com o ID: 999"));
    }

    @Test
    @DisplayName("POST /api/v1/pedidos - Com dados validos deve criar pedido e retornar 201 Created")
    void criarPedido_ComDadosValidos_DeveRetornar201() throws Exception {
        when(pedidoService.criarPedido(any(PedidoRequestDTO.class))).thenReturn(criarPedidoExemplo());

        String jsonValido = """
            {
                "clienteId": 1,
                "itens": [
                    {
                        "pratoId": 10,
                        "quantidade": 2
                    }
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.valorTotal").value(50.0));
    }

    @Test
    @DisplayName("POST /api/v1/pedidos - Com lista de itens vazia deve retornar 400 Bad Request")
    void criarPedido_ComItensVazio_DeveRetornar400() throws Exception {
        String jsonInvalido = """
            {
                "clienteId": 1,
                "itens": []
            }
            """;

        mockMvc.perform(post("/api/v1/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.itens").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/pedidos/{id}/status - Deve atualizar status e retornar 200 OK")
    void atualizarStatus_DeveRetornar200() throws Exception {
        Pedido atualizado = criarPedidoExemplo();
        atualizado.setStatus(StatusPedido.EM_PREPARO);

        when(pedidoService.atualizarStatus(eq(100L), eq(StatusPedido.EM_PREPARO))).thenReturn(atualizado);

        mockMvc.perform(patch("/api/v1/pedidos/100/status")
                .param("status", "EM_PREPARO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PREPARO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/pedidos/{id}/cancelar - Deve cancelar pedido e retornar 200 OK")
    void cancelarPedido_DeveRetornar200() throws Exception {
        Pedido cancelado = criarPedidoExemplo();
        cancelado.setStatus(StatusPedido.CANCELADO);

        when(pedidoService.cancelarPedido(100L)).thenReturn(cancelado);

        mockMvc.perform(patch("/api/v1/pedidos/100/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }
}
