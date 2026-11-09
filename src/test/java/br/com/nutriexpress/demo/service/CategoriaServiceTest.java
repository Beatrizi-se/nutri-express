package br.com.nutriexpress.demo.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.nutriexpress.demo.dto.categoria.CategoriaRequestDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.repository.CategoriaRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository repository;

    @InjectMocks
    private CategoriaService service;

    private Categoria categoriaExemplo;

    @BeforeEach
    void setUp() {
        categoriaExemplo = new Categoria(1L, "Saladas", "Saladas frescas e saudáveis");
    }

    @Test
    @DisplayName("Deve listar todas as categorias com sucesso")
    void listarCategorias_DeveRetornarListaDeCategorias() {
        when(repository.findAll()).thenReturn(List.of(
            categoriaExemplo,
            new Categoria(2L, "Sopas", "Sopas nutritivas")
        ));

        List<Categoria> resultado = service.listarCategorias();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Saladas", resultado.get(0).getNome());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar categoria por ID quando ela existir")
    void getCategoriaById_QuandoExiste_DeveRetornarCategoria() {
        when(repository.findById(1L)).thenReturn(Optional.of(categoriaExemplo));

        Categoria resultado = service.getCategoriaById(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Saladas", resultado.getNome());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ID inexistente")
    void getCategoriaById_QuandoNaoExiste_DeveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> service.getCategoriaById(99L)
        );

        assertTrue(exception.getMessage().contains("Categoria não encontrada com o ID: 99"));
        verify(repository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve cadastrar nova categoria quando nome não for duplicado")
    void criarCategoria_ComSucesso_DeveSalvarERetornar() {
        CategoriaRequestDTO request = new CategoriaRequestDTO("Bebidas", "Sucos naturais");

        when(repository.existsByNomeIgnoreCase("Bebidas")).thenReturn(false);
        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        Categoria resultado = service.criarCategoria(request);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Bebidas", resultado.getNome());
        assertEquals("Sucos naturais", resultado.getDescricao());
        verify(repository, times(1)).existsByNomeIgnoreCase("Bebidas");
        verify(repository, times(1)).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar cadastrar categoria com nome já existente")
    void criarCategoria_QuandoNomeDuplicado_DeveLancarRegraNegocioException() {
        CategoriaRequestDTO request = new CategoriaRequestDTO("Saladas", "Outra descrição");

        when(repository.existsByNomeIgnoreCase("Saladas")).thenReturn(true);

        RegraNegocioException exception = assertThrows(
            RegraNegocioException.class,
            () -> service.criarCategoria(request)
        );

        assertTrue(exception.getMessage().contains("Já existe uma categoria cadastrada com o nome: Saladas"));
        verify(repository, times(1)).existsByNomeIgnoreCase("Saladas");
        verify(repository, never()).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Deve atualizar categoria existente com sucesso")
    void atualizarCategoria_ComSucesso_DeveAtualizarERetornar() {
        CategoriaRequestDTO request = new CategoriaRequestDTO("Saladas Especiais", "Nova descrição");

        when(repository.findById(1L)).thenReturn(Optional.of(categoriaExemplo));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Saladas Especiais", 1L)).thenReturn(false);
        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Categoria resultado = service.atualizarCategoria(1L, request);

        assertNotNull(resultado);
        assertEquals("Saladas Especiais", resultado.getNome());
        assertEquals("Nova descrição", resultado.getDescricao());
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(categoriaExemplo);
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao atualizar para nome que já pertence a outra categoria")
    void atualizarCategoria_QuandoNomePertenceAOutraCategoria_DeveLancarRegraNegocioException() {
        CategoriaRequestDTO request = new CategoriaRequestDTO("Sopas", "Tentando usar nome existente");

        when(repository.findById(1L)).thenReturn(Optional.of(categoriaExemplo));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Sopas", 1L)).thenReturn(true);

        RegraNegocioException exception = assertThrows(
            RegraNegocioException.class,
            () -> service.atualizarCategoria(1L, request)
        );

        assertTrue(exception.getMessage().contains("Já existe outra categoria cadastrada com o nome: Sopas"));
        verify(repository, never()).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Deve deletar categoria existente com sucesso")
    void deletarCategoria_ComSucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(categoriaExemplo));
        doNothing().when(repository).delete(categoriaExemplo);

        assertDoesNotThrow(() -> service.deletarCategoria(1L));

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).delete(categoriaExemplo);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar deletar categoria inexistente")
    void deletarCategoria_QuandoNaoExiste_DeveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> service.deletarCategoria(99L)
        );

        verify(repository, times(1)).findById(99L);
        verify(repository, never()).delete(any(Categoria.class));
    }
}

