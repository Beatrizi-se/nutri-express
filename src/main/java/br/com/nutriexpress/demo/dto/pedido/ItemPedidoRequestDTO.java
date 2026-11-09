package br.com.nutriexpress.demo.dto.pedido;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequestDTO(
    @NotNull(message = "O ID do prato é obrigatório")
    Long pratoId,

    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade mínima por item é 1")
    Integer quantidade
) {}
