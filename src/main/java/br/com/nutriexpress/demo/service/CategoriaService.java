package br.com.nutriexpress.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.nutriexpress.demo.dto.categoria.CategoriaRequestDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.repository.CategoriaRepository;

/**
 * Camada de serviço responsável pelas regras de negócio de Categoria.
 */
@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<Categoria> listarCategorias() {
        return repository.findAll();
    }

    public Categoria getCategoriaById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));
    }

    @Transactional
    public Categoria criarCategoria(CategoriaRequestDTO cat) {
        // Validação de regra de negócio: não permitir nomes duplicados
        if (repository.existsByNomeIgnoreCase(cat.nome())) {
            throw new RegraNegocioException("Já existe uma categoria cadastrada com o nome: " + cat.nome());
        }

        Categoria categoria = new Categoria();
        categoria.setNome(cat.nome());
        categoria.setDescricao(cat.descricao());

        return repository.save(categoria);
    }

    @Transactional
    public Categoria atualizarCategoria(Long id, CategoriaRequestDTO cat) {
        Categoria categoriaExistente = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        // Validação: não permitir duplicidade com outra categoria diferente desta
        if (repository.existsByNomeIgnoreCaseAndIdNot(cat.nome(), id)) {
            throw new RegraNegocioException("Já existe outra categoria cadastrada com o nome: " + cat.nome());
        }

        categoriaExistente.setNome(cat.nome());
        categoriaExistente.setDescricao(cat.descricao());

        return repository.save(categoriaExistente);
    }

    @Transactional
    public void deletarCategoria(Long id) {
        Categoria categoriaExistente = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        repository.delete(categoriaExistente);
    }
}
