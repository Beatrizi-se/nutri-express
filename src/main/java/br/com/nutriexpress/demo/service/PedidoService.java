package br.com.nutriexpress.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.nutriexpress.demo.dto.pedido.ItemPedidoRequestDTO;
import br.com.nutriexpress.demo.dto.pedido.PedidoRequestDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.model.ItemPedido;
import br.com.nutriexpress.demo.model.Pedido;
import br.com.nutriexpress.demo.model.Prato;
import br.com.nutriexpress.demo.model.StatusPedido;
import br.com.nutriexpress.demo.repository.ClienteRepository;
import br.com.nutriexpress.demo.repository.PedidoRepository;
import br.com.nutriexpress.demo.repository.PratoRepository;

/**
 * Camada de serviço responsável pelo fluxo de pedidos (Checkout) e integração com Estoque.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final PratoRepository pratoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         PratoRepository pratoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.pratoRepository = pratoRepository;
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + id));
    }

    public List<Pedido> listarPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente não encontrado com o ID: " + clienteId);
        }
        return pedidoRepository.findByClienteId(clienteId);
    }

    public List<Pedido> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status);
    }

    /**
     * Orquestração do Checkout com Baixa Automática de Estoque:
     * 1. Valida se o cliente existe e se está ativo (Soft Delete)
     * 2. Captura o snapshot do endereço de entrega
     * 3. Valida pratos disponíveis e quantidade em estoque
     * 4. Dá baixa automática no estoque de cada prato
     * 5. Congela o preço unitário histórico de cada item
     * 6. Calcula subtotais e o total geral
     */
    @Transactional
    public Pedido criarPedido(PedidoRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com o ID: " + dto.clienteId()));

        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new RegraNegocioException("Não é possível realizar o pedido: o cliente está inativo no sistema.");
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setEnderecoEntrega(cliente.getEndereco());

        for (ItemPedidoRequestDTO itemDto : dto.itens()) {
            Prato prato = pratoRepository.findById(itemDto.pratoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com o ID: " + itemDto.pratoId()));

            if (!Boolean.TRUE.equals(prato.getDisponivel())) {
                throw new RegraNegocioException("O prato '" + prato.getNome() + "' não está disponível para pedidos.");
            }

            // Baixa automática de estoque (lança RegraNegocioException se o estoque for insuficiente)
            prato.decrementarEstoque(itemDto.quantidade());
            pratoRepository.save(prato);

            ItemPedido item = new ItemPedido(
                    prato.getId(),
                    prato.getNome(),
                    itemDto.quantidade(),
                    prato.getPreco()
            );

            pedido.adicionarItem(item);
        }

        return pedidoRepository.save(pedido);
    }

    /**
     * Atualização controlada de status de entrega (Máquina de Estados)
     */
    @Transactional
    public Pedido atualizarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);

        if (pedido.getStatus() == StatusPedido.ENTREGUE || pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Não é possível alterar o status de um pedido que já foi finalizado como " + pedido.getStatus());
        }

        pedido.setStatus(novoStatus);
        return pedidoRepository.save(pedido);
    }

    /**
     * Cancelamento de pedido com estorno automático de estoque
     */
    @Transactional
    public Pedido cancelarPedido(Long id) {
        Pedido pedido = buscarPorId(id);

        if (pedido.getStatus() == StatusPedido.ENTREGUE || pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Não é possível cancelar um pedido que já foi finalizado como " + pedido.getStatus());
        }

        // Estorno automático dos itens para o estoque
        for (ItemPedido item : pedido.getItens()) {
            pratoRepository.findById(item.getPratoId()).ifPresent(prato -> {
                prato.incrementarEstoque(item.getQuantidade());
                pratoRepository.save(prato);
            });
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        return pedidoRepository.save(pedido);
    }
}
