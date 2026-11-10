package br.com.nutriexpress.demo.dto.pedido;

import java.time.LocalDateTime;
import java.util.List;

import br.com.nutriexpress.demo.dto.cliente.EnderecoDTO;
import br.com.nutriexpress.demo.model.Pedido;
import br.com.nutriexpress.demo.model.StatusPedido;

public record PedidoResponseDTO(
    Long id,
    Long clienteId,
    String clienteNome,
    LocalDateTime dataHora,
    StatusPedido status,
    EnderecoDTO enderecoEntrega,
    List<ItemPedidoResponseDTO> itens,
    Double valorTotal
) {
    public PedidoResponseDTO(Pedido pedido) {
        this(
            pedido.getId(),
            pedido.getCliente() != null ? pedido.getCliente().getId() : null,
            pedido.getClienteNome(),
            pedido.getDataHora(),
            pedido.getStatus(),
            pedido.getEnderecoEntrega() != null ? new EnderecoDTO(pedido.getEnderecoEntrega()) : null,
            pedido.getItens() != null
                ? pedido.getItens().stream().map(ItemPedidoResponseDTO::new).toList()
                : List.of(),
            pedido.getValorTotal()
        );
    }
}
