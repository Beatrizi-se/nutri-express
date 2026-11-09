package br.com.nutriexpress.demo.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.nutriexpress.demo.dto.cliente.ClienteRequestDTO;
import br.com.nutriexpress.demo.dto.cliente.EnderecoDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.model.Endereco;
import br.com.nutriexpress.demo.repository.ClienteRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteService service;

    private Cliente clienteExemplo;
    private EnderecoDTO enderecoDTO;

    @BeforeEach
    void setUp() {
        Endereco endereco = new Endereco("Rua das Flores", "123", "Apto 4", "Centro", "São Paulo", "01001-000");
        clienteExemplo = new Cliente(1L, "Maria Silva", "maria@email.com", "12345678901", "11999998888", true, endereco);
        enderecoDTO = new EnderecoDTO("Rua das Flores", "123", "Apto 4", "Centro", "São Paulo", "01001-000");
    }

    @Test
    @DisplayName("Deve listar todos os clientes ativos")
    void listarAtivos_DeveRetornarApenasClientesAtivos() {
        when(repository.findByAtivoTrue()).thenReturn(List.of(clienteExemplo));

        List<Cliente> resultado = service.listarAtivos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getAtivo());
        verify(repository, times(1)).findByAtivoTrue();
    }

    @Test
    @DisplayName("Deve buscar cliente por ID quando existir")
    void buscarPorId_QuandoExiste_DeveRetornarCliente() {
        when(repository.findById(1L)).thenReturn(Optional.of(clienteExemplo));

        Cliente resultado = service.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("Maria Silva", resultado.getNome());
        assertEquals("maria@email.com", resultado.getEmail());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ID inexistente")
    void buscarPorId_QuandoNaoExiste_DeveLancarExcecao() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99L));
    }

    @Test
    @DisplayName("Deve cadastrar cliente com sucesso quando email e cpf forem unicos")
    void criarCliente_ComSucesso() {
        ClienteRequestDTO request = new ClienteRequestDTO("João Souza", "joao@email.com", "98765432100", "11988887777", enderecoDTO);

        when(repository.existsByEmail("joao@email.com")).thenReturn(false);
        when(repository.existsByCpf("98765432100")).thenReturn(false);
        when(repository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        Cliente criado = service.criarCliente(request);

        assertNotNull(criado);
        assertEquals(2L, criado.getId());
        assertEquals("João Souza", criado.getNome());
        assertTrue(criado.getAtivo());
        verify(repository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar cadastrar cliente com email duplicado")
    void criarCliente_EmailDuplicado_DeveLancarExcecao() {
        ClienteRequestDTO request = new ClienteRequestDTO("João Souza", "maria@email.com", "98765432100", "11988887777", enderecoDTO);

        when(repository.existsByEmail("maria@email.com")).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.criarCliente(request));
        assertTrue(ex.getMessage().contains("Já existe um cliente cadastrado com o e-mail"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar cadastrar cliente com CPF duplicado")
    void criarCliente_CpfDuplicado_DeveLancarExcecao() {
        ClienteRequestDTO request = new ClienteRequestDTO("João Souza", "joao@email.com", "12345678901", "11988887777", enderecoDTO);

        when(repository.existsByEmail("joao@email.com")).thenReturn(false);
        when(repository.existsByCpf("12345678901")).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.criarCliente(request));
        assertTrue(ex.getMessage().contains("Já existe um cliente cadastrado com o CPF"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve inativar cliente com sucesso (Soft Delete)")
    void inativarCliente_ComSucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(clienteExemplo));
        when(repository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.inativarCliente(1L);

        assertFalse(clienteExemplo.getAtivo());
        verify(repository, times(1)).save(clienteExemplo);
    }

    @Test
    @DisplayName("Deve reativar cliente com sucesso")
    void reativarCliente_ComSucesso() {
        clienteExemplo.setAtivo(false);
        when(repository.findById(1L)).thenReturn(Optional.of(clienteExemplo));
        when(repository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.reativarCliente(1L);

        assertTrue(clienteExemplo.getAtivo());
        verify(repository, times(1)).save(clienteExemplo);
    }
}
