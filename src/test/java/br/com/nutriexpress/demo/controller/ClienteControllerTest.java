package br.com.nutriexpress.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.nutriexpress.demo.dto.cliente.ClienteRequestDTO;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.model.Endereco;
import br.com.nutriexpress.demo.repository.UsuarioRepository;
import br.com.nutriexpress.demo.service.ClienteService;
import br.com.nutriexpress.demo.service.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integracao da camada web para ClienteController usando MockMvc.
 */
@WebMvcTest(controllers = ClienteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Endereco criarEndereco() {
        return new Endereco("Rua das Flores", "123", "Apto 4", "Centro", "São Paulo", "01001-000");
    }

    private Cliente criarClienteExemplo() {
        return new Cliente(1L, "Maria Silva", "maria@email.com", "12345678901", "11988887777",
                LocalDate.of(1995, 5, 20), true, criarEndereco());
    }

    @Test
    @DisplayName("GET /api/v1/clientes - Deve retornar 200 OK com a lista de clientes")
    void listarClientes_DeveRetornar200ComLista() throws Exception {
        when(clienteService.listarAtivos()).thenReturn(List.of(criarClienteExemplo()));

        mockMvc.perform(get("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Maria Silva"))
                .andExpect(jsonPath("$[0].email").value("maria@email.com"))
                .andExpect(jsonPath("$[0].dataNascimento").value("1995-05-20"))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} - Deve retornar 200 OK quando ID existir")
    void buscarPorId_QuandoExiste_DeveRetornar200() throws Exception {
        when(clienteService.buscarPorId(1L)).thenReturn(criarClienteExemplo());

        mockMvc.perform(get("/api/v1/clientes/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} - Deve retornar 404 Not Found quando ID nao existir")
    void buscarPorId_QuandoNaoExiste_DeveRetornar404() throws Exception {
        when(clienteService.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado com o ID: 99"));

        mockMvc.perform(get("/api/v1/clientes/99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente não encontrado com o ID: 99"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes - Com dados validos deve cadastrar e retornar 201 Created")
    void criarCliente_ComDadosValidos_DeveRetornar201() throws Exception {
        when(clienteService.criarCliente(any(ClienteRequestDTO.class))).thenReturn(criarClienteExemplo());

        String jsonValido = """
            {
                "nome": "Maria Silva",
                "email": "maria@email.com",
                "cpf": "12345678901",
                "telefone": "11988887777",
                "dataNascimento": "1995-05-20",
                "endereco": {
                    "logradouro": "Rua das Flores",
                    "numero": "123",
                    "complemento": "Apto 4",
                    "bairro": "Centro",
                    "cidade": "São Paulo",
                    "cep": "01001-000"
                }
            }
            """;

        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.dataNascimento").value("1995-05-20"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes - Com CEP invalido deve retornar 400 Bad Request")
    void criarCliente_ComCepInvalido_DeveRetornar400() throws Exception {
        String jsonInvalido = """
            {
                "nome": "Maria Silva",
                "email": "maria@email.com",
                "cpf": "12345678901",
                "telefone": "11988887777",
                "dataNascimento": "1995-05-20",
                "endereco": {
                    "logradouro": "Rua das Flores",
                    "numero": "123",
                    "complemento": "Apto 4",
                    "bairro": "Centro",
                    "cidade": "São Paulo",
                    "cep": "123"
                }
            }
            """;

        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages['endereco.cep']").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} - Deve executar soft delete e retornar 204 No Content")
    void inativarCliente_DeveRetornar204() throws Exception {
        doNothing().when(clienteService).inativarCliente(1L);

        mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/v1/clientes/{id}/reativar - Deve reativar cliente e retornar 204 No Content")
    void reativarCliente_DeveRetornar204() throws Exception {
        doNothing().when(clienteService).reativarCliente(1L);

        mockMvc.perform(patch("/api/v1/clientes/1/reativar"))
                .andExpect(status().isNoContent());
    }
}
