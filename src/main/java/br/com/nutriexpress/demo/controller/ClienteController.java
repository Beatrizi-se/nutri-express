package br.com.nutriexpress.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.nutriexpress.demo.dto.cliente.ClienteRequestDTO;
import br.com.nutriexpress.demo.dto.cliente.ClienteResponseDTO;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.service.ClienteService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * Listagem com filtros opcionais:
     * - /api/v1/clientes (lista apenas os ativos por padrão)
     * - /api/v1/clientes?apenasAtivos=false (lista todos, inclusive inativos)
     * - /api/v1/clientes?nome=Maria
     * - /api/v1/clientes?cpf=12345678901
     * - /api/v1/clientes?email=maria@email.com
     */
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {

        if (cpf != null && !cpf.isBlank()) {
            Cliente cliente = clienteService.buscarPorCpf(cpf);
            return ResponseEntity.ok(List.of(new ClienteResponseDTO(cliente)));
        }

        if (email != null && !email.isBlank()) {
            Cliente cliente = clienteService.buscarPorEmail(email);
            return ResponseEntity.ok(List.of(new ClienteResponseDTO(cliente)));
        }

        List<Cliente> clientes;
        if (nome != null && !nome.isBlank()) {
            clientes = clienteService.buscarPorNome(nome);
        } else if (apenasAtivos) {
            clientes = clienteService.listarAtivos();
        } else {
            clientes = clienteService.listarTodos();
        }

        List<ClienteResponseDTO> dtos = clientes.stream()
                .map(ClienteResponseDTO::new)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        Cliente cliente = clienteService.buscarPorId(id);
        return ResponseEntity.ok(new ClienteResponseDTO(cliente));
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> criarCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        Cliente clienteCriado = clienteService.criarCliente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ClienteResponseDTO(clienteCriado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO dto) {
        Cliente clienteAtualizado = clienteService.atualizarCliente(id, dto);
        return ResponseEntity.ok(new ClienteResponseDTO(clienteAtualizado));
    }

    /**
     * Inativação lógica (Soft Delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativarCliente(@PathVariable Long id) {
        clienteService.inativarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<Void> reativarCliente(@PathVariable Long id) {
        clienteService.reativarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
