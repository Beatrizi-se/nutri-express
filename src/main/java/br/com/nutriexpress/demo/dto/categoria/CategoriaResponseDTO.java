package br.com.nutriexpress.demo.dto.categoria;

import br.com.nutriexpress.demo.model.Categoria;

public record CategoriaResponseDTO(
    Long id,
    String nome,
    String descricao
) {
    public CategoriaResponseDTO(Categoria categoria) {
        this(categoria.getId(), categoria.getNome(), categoria.getDescricao());
    }
}

