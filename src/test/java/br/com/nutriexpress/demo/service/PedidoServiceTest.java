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

        pratoDisponivel = new Prato(10L, "Salada Fit", "Salada completa", 25.0, 200, true, 1L, 10);
        pratoIndisponivel = new Prato(20L, "Suco Esgotado", "Indisponível hoje", 12.0, 100, false, 2L, 0);
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
        ItemPedido item = new ItemPedido(10L, "Salada Fit", 3, 25.0);
        pedido.adicionarItem(item);

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(pratoRepository.findById(10L)).thenReturn(Optional.of(pratoDisponivel)); // estoque inicial 10
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
}
