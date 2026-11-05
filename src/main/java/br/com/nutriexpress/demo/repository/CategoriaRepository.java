package br.com.nutriexpress.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.nutriexpress.demo.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    // Verifica se já existe categoria com este nome (ignorando maiúsculas/minúsculas)
    boolean existsByNomeIgnoreCase(String nome);

    // Verifica se já existe outra categoria com este nome, ignorando o próprio ID que está sendo atualizado
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
