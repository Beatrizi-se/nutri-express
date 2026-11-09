package br.com.nutriexpress.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.nutriexpress.demo.dto.prato.PratoRequestDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Prato;
import br.com.nutriexpress.demo.repository.CategoriaRepository;
import br.com.nutriexpress.demo.repository.PratoRepository;

/**
 * Camada de serviço responsável pelas regras de negócio de Prato.
 */
@Service
public class PratoService {

    private final PratoRepository pratoRepository;
    private final CategoriaRepository categoriaRepository;

    // Injeção de dependências via construtor
    public PratoService(PratoRepository pratoRepository, CategoriaRepository categoriaRepository) {
        this.pratoRepository = pratoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Prato> listarTodos() {
        return pratoRepository.findAll();
    }

    public List<Prato> listarDisponiveis() {
        return pratoRepository.findByDisponivelTrue();
    }

    public Prato buscarPorId(Long id) {
        return pratoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com o ID: " + id));
    }

    public List<Prato> buscarPorCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new ResourceNotFoundException("Categoria não encontrada com o ID: " + categoriaId);
        }
        return pratoRepository.findByCategoriaId(categoriaId);
    }

    public List<Prato> buscarPorNome(String nome) {
        return pratoRepository.findByNomeContainingIgnoreCase(nome);
    }

    public List<Prato> buscarPorCaloriasMaximas(Integer calorias) {
        return pratoRepository.findByCaloriasLessThanEqual(calorias);
    }

    @Transactional
    public Prato criarPrato(PratoRequestDTO dto) {
        // Regra 1: Validar se a categoria informada existe no banco
        if (!categoriaRepository.existsById(dto.categoriaId())) {
            throw new ResourceNotFoundException("Não é possível cadastrar o prato: Categoria não encontrada com o ID: " + dto.categoriaId());
        }

        // Regra 2: Impedir cadastro de pratos com nomes duplicados
        if (pratoRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new RegraNegocioException("Já existe um prato cadastrado com o nome: " + dto.nome());
        }

        Prato prato = new Prato();
        prato.setNome(dto.nome());
        prato.setDescricao(dto.descricao());
        prato.setPreco(dto.preco());
        prato.setCalorias(dto.calorias());
        prato.setDisponivel(dto.disponivel() != null ? dto.disponivel() : true);
        prato.setCategoriaId(dto.categoriaId());

        return pratoRepository.save(prato);
    }

    @Transactional
    public Prato atualizarPrato(Long id, PratoRequestDTO dto) {
        Prato pratoExistente = buscarPorId(id);

        // Regra 1: Validar se a nova categoria existe
        if (!categoriaRepository.existsById(dto.categoriaId())) {
            throw new ResourceNotFoundException("Não é possível atualizar o prato: Categoria não encontrada com o ID: " + dto.categoriaId());
        }

        // Regra 2: Impedir duplicação de nome com outro prato diferente deste
        if (pratoRepository.existsByNomeIgnoreCaseAndIdNot(dto.nome(), id)) {
            throw new RegraNegocioException("Já existe outro prato cadastrado com o nome: " + dto.nome());
        }

        pratoExistente.setNome(dto.nome());
        pratoExistente.setDescricao(dto.descricao());
        pratoExistente.setPreco(dto.preco());
        pratoExistente.setCalorias(dto.calorias());
        if (dto.disponivel() != null) {
            pratoExistente.setDisponivel(dto.disponivel());
        }
        pratoExistente.setCategoriaId(dto.categoriaId());

        return pratoRepository.save(pratoExistente);
    }

    @Transactional
    public Prato atualizarPreco(Long id, Double novoPreco) {
        if (novoPreco == null || novoPreco <= 0) {
            throw new RegraNegocioException("O novo preço deve ser maior que zero");
        }

        Prato pratoExistente = buscarPorId(id);
        pratoExistente.setPreco(novoPreco);

        return pratoRepository.save(pratoExistente);
    }

    @Transactional
    public void deletarPrato(Long id) {
        Prato pratoExistente = buscarPorId(id);
        pratoRepository.delete(pratoExistente);
    }
}
