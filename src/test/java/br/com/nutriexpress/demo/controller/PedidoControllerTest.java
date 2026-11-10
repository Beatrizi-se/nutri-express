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
import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.model.ItemPedido;
import br.com.nutriexpress.demo.model.Pedido;
import br.com.nutriexpress.demo.model.Prato;
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
        Cliente cliente = new Cliente(1L, "Maria Silva", "maria@email.com", "12345678901", "11999999999", null, true, null);
        Categoria categoria = new Categoria(1L, "Saladas", "Saladas");
        Prato prato = new Prato(10L, "Salada Caesar", "Salada leve", 25.0, 200, true, categoria, 10);

        Pedido pedido = new Pedido();
        pedido.setId(100L);
        pedido.setCliente(cliente);
        pedido.setClienteNome("Maria Silva");
        pedido.setDataHora(LocalDateTime.now());
        pedido.setStatus(StatusPedido.PENDENTE);

        ItemPedido item = new ItemPedido(prato, 2, 25.0);
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

    @Test
    @DisplayName("POST e GET /api/v1/pedidos - Ciclo dinâmico com inativação de cliente e preservação do histórico")
    void cicloPedido_ClienteInativado_DeveBloquearNovosEPreservarHistorico() throws Exception {
        Long clienteId = 1L;

        // 1. Simula checkout bem-sucedido com cliente ativo
        Pedido pedidoAtivo = criarPedidoExemplo();
        when(pedidoService.criarPedido(any(PedidoRequestDTO.class))).thenReturn(pedidoAtivo);

        String jsonNovoPedido = String.format("""
            {
                "clienteId": %d,
                "itens": [
                    { "pratoId": 10, "quantidade": 2 }
                ]
            }
        """, clienteId);

        String responseCriacao = mockMvc.perform(post("/api/v1/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonNovoPedido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(clienteId))
                .andExpect(jsonPath("$.clienteNome").value("Maria Silva"))
                .andReturn().getResponse().getContentAsString();

        // Extrai o ID dinâmico retornado na resposta
        Number idDinamico = com.jayway.jsonpath.JsonPath.read(responseCriacao, "$.id");
        Long pedidoIdCriado = idDinamico.longValue();

        // 2. Simula inativação do cliente: tentativa de novo pedido deve retornar 409 Conflict
        when(pedidoService.criarPedido(any(PedidoRequestDTO.class)))
                .thenThrow(new br.com.nutriexpress.demo.exception.RegraNegocioException("Não é possível realizar o pedido: o cliente está inativo no sistema."));

        mockMvc.perform(post("/api/v1/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonNovoPedido))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Não é possível realizar o pedido: o cliente está inativo no sistema."));

        // 3. Simula consulta histórica do pedido anterior pelo ID capturado dinamicamente
        when(pedidoService.buscarPorId(pedidoIdCriado)).thenReturn(pedidoAtivo);

        mockMvc.perform(get("/api/v1/pedidos/" + pedidoIdCriado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedidoIdCriado))
                .andExpect(jsonPath("$.clienteId").value(clienteId))
                .andExpect(jsonPath("$.clienteNome").value("Maria Silva"));

        // 4. Simula listagem histórica por cliente filtrado usando o clienteId dinâmico
        when(pedidoService.listarPorCliente(clienteId)).thenReturn(List.of(pedidoAtivo));

        mockMvc.perform(get("/api/v1/pedidos").param("clienteId", clienteId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedidoIdCriado))
                .andExpect(jsonPath("$[0].clienteId").value(clienteId))
                .andExpect(jsonPath("$[0].clienteNome").value("Maria Silva"));
    }
}
