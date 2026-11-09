package br.com.nutriexpress.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.nutriexpress.demo.dto.cliente.ClienteRequestDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.repository.ClienteRepository;

/**
 * Camada de serviço responsável pelas regras de negócio de Cliente.
 */
@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listarTodos() {
        return repository.findAll();
    }

    public List<Cliente> listarAtivos() {
        return repository.findByAtivoTrue();
    }

    public Cliente buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com o ID: " + id));
    }

    public Cliente buscarPorCpf(String cpf) {
        return repository.findByCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com o CPF: " + cpf));
    }

    public Cliente buscarPorEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com o e-mail: " + email));
    }

    public List<Cliente> buscarPorNome(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome);
    }

    @Transactional
    public Cliente criarCliente(ClienteRequestDTO dto) {
        // Validação 1: E-mail único
        if (repository.existsByEmail(dto.email())) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com o e-mail: " + dto.email());
        }

        // Validação 2: CPF único
        if (repository.existsByCpf(dto.cpf())) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com o CPF: " + dto.cpf());
        }

        Cliente cliente = new Cliente();
        cliente.setNome(dto.nome());
        cliente.setEmail(dto.email());
        cliente.setCpf(dto.cpf());
        cliente.setTelefone(dto.telefone());
        cliente.setDataNascimento(dto.dataNascimento());
        cliente.setAtivo(true);
        cliente.setEndereco(dto.endereco().toEntity());

        return repository.save(cliente);
    }

    @Transactional
    public Cliente atualizarCliente(Long id, ClienteRequestDTO dto) {
        Cliente clienteExistente = buscarPorId(id);

        // Validação de duplicidade ignorando o próprio ID
        if (repository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new RegraNegocioException("Já existe outro cliente cadastrado com o e-mail: " + dto.email());
        }

        if (repository.existsByCpfAndIdNot(dto.cpf(), id)) {
            throw new RegraNegocioException("Já existe outro cliente cadastrado com o CPF: " + dto.cpf());
        }

        clienteExistente.setNome(dto.nome());
        clienteExistente.setEmail(dto.email());
        clienteExistente.setCpf(dto.cpf());
        clienteExistente.setTelefone(dto.telefone());
        clienteExistente.setDataNascimento(dto.dataNascimento());
        clienteExistente.setEndereco(dto.endereco().toEntity());

        return repository.save(clienteExistente);
    }

    /**
     * Soft Delete: Em vez de apagar do banco, inativa o cadastro para preservar
     * a integridade dos pedidos e relatórios financeiros.
     */
    @Transactional
    public void inativarCliente(Long id) {
        Cliente clienteExistente = buscarPorId(id);
        clienteExistente.setAtivo(false);
        repository.save(clienteExistente);
    }

    @Transactional
    public void reativarCliente(Long id) {
        Cliente clienteExistente = buscarPorId(id);
        clienteExistente.setAtivo(true);
        repository.save(clienteExistente);
    }
}
