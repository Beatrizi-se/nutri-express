package br.com.nutriexpress.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.nutriexpress.demo.dto.pedido.ItemPedidoRequestDTO;
import br.com.nutriexpress.demo.dto.pedido.PedidoRequestDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.model.Endereco;
import br.com.nutriexpress.demo.model.ItemPedido;
import br.com.nutriexpress.demo.model.Pedido;
import br.com.nutriexpress.demo.model.Prato;
import br.com.nutriexpress.demo.model.StatusPedido;
import br.com.nutriexpress.demo.repository.ClienteRepository;
import br.com.nutriexpress.demo.repository.PedidoRepository;
import br.com.nutriexpress.demo.repository.PratoRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PratoRepository pratoRepository;

    @InjectMocks
    private PedidoService pedidoService;

    private Cliente clienteAtivo;
    private Cliente clienteInativo;
    private Prato pratoDisponivel;
    private Prato pratoIndisponivel;

    @BeforeEach
    void setUp() {
        Endereco endereco = new Endereco("Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "01310-100");
        clienteAtivo = new Cliente(1L, "Carlos Lima", "carlos@email.com", "11122233344", "11987654321", LocalDate.of(1990, 1, 1), true, endereco);
        clienteInativo = new Cliente(2L, "Ana Inativa", "ana@email.com", "55566677788", "11912345678", LocalDate.of(1992, 2, 2), false, endereco);

        Categoria cat1 = new Categoria(1L, "Saladas", "Saladas");
        Categoria cat2 = new Categoria(2L, "Bebidas", "Sucos");
        pratoDisponivel = new Prato(10L, "Salada Fit", "Salada completa", 25.0, 200, true, cat1, 10);
        pratoIndisponivel = new Prato(20L, "Suco Esgotado", "Indisponível hoje", 12.0, 100, false, cat2, 0);
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso, dar baixa no estoque e calcular valor total")
    void criarPedido_ComSucesso() {
        PedidoRequestDTO request = new PedidoRequestDTO(
                1L,
                List.of(new ItemPedidoRequestDTO(10L, 2)) // 2 x 25.0 = 50.0
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteAtivo));
        when(pratoRepository.findById(10L)).thenReturn(Optional.of(pratoDisponivel));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        Pedido pedidoCriado = pedidoService.criarPedido(request);

        assertNotNull(pedidoCriado);
        assertEquals(100L, pedidoCriado.getId());
        assertEquals("Carlos Lima", pedidoCriado.getClienteNome());
        assertEquals(StatusPedido.PENDENTE, pedidoCriado.getStatus());
        assertEquals(1, pedidoCriado.getItens().size());
        assertEquals(50.0, pedidoCriado.getValorTotal());
        assertEquals(8, pratoDisponivel.getQuantidadeEstoque()); // Baixa de 10 para 8!
        verify(pratoRepository, times(1)).save(pratoDisponivel);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar criar pedido com estoque insuficiente")
    void criarPedido_EstoqueInsuficiente_DeveLancarExcecao() {
        PedidoRequestDTO request = new PedidoRequestDTO(
                1L,
                List.of(new ItemPedidoRequestDTO(10L, 15)) // Estoque tem 10, solicitou 15!
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteAtivo));
        when(pratoRepository.findById(10L)).thenReturn(Optional.of(pratoDisponivel));

        RegraNegocioException ex = assertThrows(
                RegraNegocioException.class,
                () -> pedidoService.criarPedido(request)
        );

        assertTrue(ex.getMessage().contains("Estoque insuficiente"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar criar pedido para cliente inativo (Soft Delete)")
    void criarPedido_ClienteInativo_DeveLancarExcecao() {
        PedidoRequestDTO request = new PedidoRequestDTO(2L, List.of(new ItemPedidoRequestDTO(10L, 1)));

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInativo));

        RegraNegocioException ex = assertThrows(
                RegraNegocioException.class,
                () -> pedidoService.criarPedido(request)
        );

        assertTrue(ex.getMessage().contains("o cliente está inativo no sistema"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar adicionar prato indisponível ao pedido")
    void criarPedido_PratoIndisponivel_DeveLancarExcecao() {
        PedidoRequestDTO request = new PedidoRequestDTO(1L, List.of(new ItemPedidoRequestDTO(20L, 1)));

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteAtivo));
        when(pratoRepository.findById(20L)).thenReturn(Optional.of(pratoIndisponivel));

        RegraNegocioException ex = assertThrows(
                RegraNegocioException.class,
                () -> pedidoService.criarPedido(request)
        );

        assertTrue(ex.getMessage().contains("não está disponível para pedidos"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar status do pedido com sucesso")
    void atualizarStatus_ComSucesso() {
        Pedido pedido = new Pedido();
        pedido.setId(100L);
        pedido.setStatus(StatusPedido.PENDENTE);

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido atualizado = pedidoService.atualizarStatus(100L, StatusPedido.EM_PREPARO);

        assertEquals(StatusPedido.EM_PREPARO, atualizado.getStatus());
        verify(pedidoRepository, times(1)).save(pedido);
    }

    @Test
    @DisplayName("Deve cancelar pedido e estornar itens de volta ao estoque")
    void cancelarPedido_ComSucesso_DeveEstornarEstoque() {
        Pedido pedido = new Pedido();
        pedido.setId(100L);
        pedido.setStatus(StatusPedido.PENDENTE);
        ItemPedido item = new ItemPedido(pratoDisponivel, 3, 25.0);
        pedido.adicionarItem(item);

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido cancelado = pedidoService.cancelarPedido(100L);

        assertEquals(StatusPedido.CANCELADO, cancelado.getStatus());
        assertEquals(13, pratoDisponivel.getQuantidadeEstoque()); // Estornou +3 itens: 10 + 3 = 13!
        verify(pratoRepository, times(1)).save(pratoDisponivel);
        verify(pedidoRepository, times(1)).save(pedido);
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar alterar status de pedido já entregue")
    void atualizarStatus_PedidoJaEntregue_DeveLancarExcecao() {
        Pedido pedido = new Pedido();
        pedido.setId(100L);
        pedido.setStatus(StatusPedido.ENTREGUE);

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(
                RegraNegocioException.class,
                () -> pedidoService.atualizarStatus(100L, StatusPedido.A_CAMINHO)
        );

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve vincular cliente na criação do pedido e manter integridade bidirecional")
    void criarPedido_DeveVincularClienteEColecaoBidirecional() {
        PedidoRequestDTO request = new PedidoRequestDTO(
                1L,
                List.of(new ItemPedidoRequestDTO(10L, 1))
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteAtivo));
        when(pratoRepository.findById(10L)).thenReturn(Optional.of(pratoDisponivel));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido criado = pedidoService.criarPedido(request);

        assertNotNull(criado);
        assertEquals(clienteAtivo, criado.getCliente());
        assertEquals("Carlos Lima", criado.getClienteNome());
        assertTrue(clienteAtivo.getPedidos().contains(criado));
    }

    @Test
    @DisplayName("Deve manter referência original estável e não poluir coleção de clone com mesmo ID")
    void setCliente_ComMesmoIdEmInstanciasDiferentes_DeveManterReferenciaOriginalEIsolarColecoes() {
        Cliente clienteOriginal = new Cliente(1L, "Carlos", "carlos@email.com", "111", "999", null, true, null);
        Cliente clienteClone = new Cliente(1L, "Carlos Clone", "carlos@email.com", "111", "999", null, true, null);

        Pedido pedido = new Pedido();
        pedido.setCliente(clienteOriginal);

        // Segunda chamada com instância clone de mesmo ID
        assertDoesNotThrow(() -> pedido.setCliente(clienteClone));

        // Asserções estritas exigidas:
        assertSame(clienteOriginal, pedido.getCliente(), "A referência do pedido deve continuar sendo o clienteOriginal");
        assertTrue(clienteOriginal.getPedidos().contains(pedido), "clienteOriginal deve conter o pedido em sua coleção");
        assertFalse(clienteClone.getPedidos().contains(pedido), "clienteClone NÃO deve conter o pedido em sua coleção");
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar transferir pedido para outro cliente")
    void setCliente_ComClientesDiferentes_DeveLancarExcecao() {
        Cliente clienteA = new Cliente(1L, "Cliente A", "a@email.com", "111", "999", null, true, null);
        Cliente clienteB = new Cliente(2L, "Cliente B", "b@email.com", "222", "888", null, true, null);

        Pedido pedido = new Pedido();
        pedido.setCliente(clienteA);

        assertThrows(RegraNegocioException.class, () -> pedido.setCliente(clienteB));
        assertSame(clienteA, pedido.getCliente());
    }

    @Test
    @DisplayName("Deve rejeitar cliente nulo e proteger encapsulamento da lista de pedidos")
    void regrasAssociacao_RejeitaNuloEImutabilidadeColecao() {
        Pedido pedido = new Pedido();
        assertThrows(IllegalArgumentException.class, () -> pedido.setCliente(null));

        Cliente cliente = new Cliente(1L, "Carlos", "carlos@email.com", "111", "999", null, true, null);
        assertThrows(IllegalArgumentException.class, () -> cliente.adicionarPedido(null));

        cliente.adicionarPedido(pedido);
        List<Pedido> lista = cliente.getPedidos();
        assertThrows(UnsupportedOperationException.class, () -> lista.add(new Pedido()));
    }

    @Test
    @DisplayName("Deve validar regras de criação de ItemPedido e imutabilidade de prato")
    void itemPedido_DeveValidarRegrasDeNegocio() {
        // 1. Rejeita prato nulo
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(null, 2, 25.0));

        // 2. Rejeita quantidade nula, negativa ou zero
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(pratoDisponivel, null, 25.0));
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(pratoDisponivel, 0, 25.0));
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(pratoDisponivel, -3, 25.0));

        // 3. Rejeita preço unitário nulo, negativo ou zero
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(pratoDisponivel, 2, null));
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(pratoDisponivel, 2, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new ItemPedido(pratoDisponivel, 2, -10.0));

        // 4. Criação válida e congelamento de snapshot
        ItemPedido item = new ItemPedido(pratoDisponivel, 2, 25.0);
        assertEquals(50.0, item.getSubtotal());
        assertEquals("Salada Fit", item.getPratoNome());
        assertSame(pratoDisponivel, item.getPrato());

        // 5. Simula reajuste do prato no catálogo e comprova que o snapshot do item não muda
        pratoDisponivel.setPreco(45.0);
        assertEquals(25.0, item.getPrecoUnitario());
        assertEquals(50.0, item.getSubtotal());

        // 6. Idempotência em setPrato com mesma instância ou clone
        assertDoesNotThrow(() -> item.setPrato(pratoDisponivel));
        Prato pratoClone = new Prato(10L, "Salada Fit Clone", "Outra", 50.0, 300, true, null, 10);
        assertDoesNotThrow(() -> item.setPrato(pratoClone));

        // 7. Bloqueio ao tentar trocar para outro prato de ID diferente
        Categoria cat = new Categoria(5L, "Sobremesas", "");
        Prato outroPrato = new Prato(99L, "Torta Fit", "Torta", 18.0, 200, true, cat, 5);
        assertThrows(RegraNegocioException.class, () -> item.setPrato(outroPrato));
    }
}
