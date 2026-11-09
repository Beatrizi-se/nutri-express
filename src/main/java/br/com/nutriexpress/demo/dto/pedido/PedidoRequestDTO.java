package br.com.nutriexpress.demo.dto.pedido;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PedidoRequestDTO(
    @NotNull(message = "O ID do cliente é obrigatório")
    Long clienteId,

    @NotEmpty(message = "O pedido deve conter pelo menos um item")
    @Valid
    List<ItemPedidoRequestDTO> itens
) {}
