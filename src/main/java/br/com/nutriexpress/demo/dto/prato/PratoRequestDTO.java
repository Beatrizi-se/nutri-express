package br.com.nutriexpress.demo.dto.prato;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PratoRequestDTO(
    @NotBlank(message = "O nome do prato é obrigatório")
    @Size(max = 100, message = "O nome do prato deve ter no máximo 100 caracteres")
    String nome,

    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    String descricao,

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    Double preco,

    @PositiveOrZero(message = "As calorias não podem ser negativas")
    Integer calorias,

    Boolean disponivel,

    @NotNull(message = "O ID da categoria é obrigatório")
    Long categoriaId,

    @PositiveOrZero(message = "A quantidade em estoque não pode ser negativa")
    Integer quantidadeEstoque
) {
    // Construtor de compatibilidade caso quantidadeEstoque seja omitida
    public PratoRequestDTO(String nome, String descricao, Double preco, Integer calorias, Boolean disponivel, Long categoriaId) {
        this(nome, descricao, preco, calorias, disponivel, categoriaId, 0);
    }
}
