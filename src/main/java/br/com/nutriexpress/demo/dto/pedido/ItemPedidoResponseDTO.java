package br.com.nutriexpress.demo.dto.pedido;

import br.com.nutriexpress.demo.model.ItemPedido;

public record ItemPedidoResponseDTO(
    Long id,
    Long pratoId,
    String pratoNome,
    Integer quantidade,
    Double precoUnitario,
    Double subtotal
) {
    public ItemPedidoResponseDTO(ItemPedido item) {
        this(
            item.getId(),
            item.getPrato() != null ? item.getPrato().getId() : null,
            item.getPratoNome(),
            item.getQuantidade(),
            item.getPrecoUnitario(),
            item.getSubtotal()
        );
    }
}
