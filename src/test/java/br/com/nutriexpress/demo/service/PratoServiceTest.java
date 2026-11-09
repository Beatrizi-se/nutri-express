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

import br.com.nutriexpress.demo.dto.prato.PratoRequestDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.exception.ResourceNotFoundException;
import br.com.nutriexpress.demo.model.Prato;
import br.com.nutriexpress.demo.repository.CategoriaRepository;
import br.com.nutriexpress.demo.repository.PratoRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PratoServiceTest {

    @Mock
    private PratoRepository pratoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private PratoService pratoService;

    private Prato pratoExemplo;

    @BeforeEach
    void setUp() {
        pratoExemplo = new Prato(1L, "Salada Caesar", "Alface, frango e molho especial", 28.50, 350, true, 1L);
    }

    @Test
    @DisplayName("Deve listar todos os pratos")
    void listarTodos_DeveRetornarLista() {
        when(pratoRepository.findAll()).thenReturn(List.of(pratoExemplo));

        List<Prato> resultado = pratoService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Salada Caesar", resultado.get(0).getNome());
        verify(pratoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve listar apenas pratos disponíveis")
    void listarDisponiveis_DeveRetornarApenasAtivos() {
        when(pratoRepository.findByDisponivelTrue()).thenReturn(List.of(pratoExemplo));

        List<Prato> resultado = pratoService.listarDisponiveis();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getDisponivel());
        verify(pratoRepository, times(1)).findByDisponivelTrue();
    }

    @Test
    @DisplayName("Deve buscar prato por ID quando existir")
    void buscarPorId_QuandoExiste_DeveRetornarPrato() {
        when(pratoRepository.findById(1L)).thenReturn(Optional.of(pratoExemplo));

        Prato resultado = pratoService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Salada Caesar", resultado.getNome());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar prato inexistente")
    void buscarPorId_QuandoNaoExiste_DeveLancarExcecao() {
        when(pratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pratoService.buscarPorId(99L));
    }

    @Test
    @DisplayName("Deve cadastrar prato com sucesso quando categoria existe e nome não é duplicado")
    void criarPrato_ComSucesso() {
        PratoRequestDTO request = new PratoRequestDTO("Wrap de Frango", "Wrap integral", 22.0, 310, true, 1L);

        when(categoriaRepository.existsById(1L)).thenReturn(true);
        when(pratoRepository.existsByNomeIgnoreCase("Wrap de Frango")).thenReturn(false);
        when(pratoRepository.save(any(Prato.class))).thenAnswer(invocation -> {
            Prato p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        Prato criado = pratoService.criarPrato(request);

        assertNotNull(criado);
        assertEquals(2L, criado.getId());
        assertEquals("Wrap de Frango", criado.getNome());
        verify(categoriaRepository, times(1)).existsById(1L);
        verify(pratoRepository, times(1)).save(any(Prato.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar criar prato para categoria inexistente")
    void criarPrato_QuandoCategoriaNaoExiste_DeveLancarExcecao() {
        PratoRequestDTO request = new PratoRequestDTO("Wrap de Frango", "Wrap integral", 22.0, 310, true, 999L);

        when(categoriaRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> pratoService.criarPrato(request));
        verify(pratoRepository, never()).save(any(Prato.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar cadastrar prato com nome duplicado")
    void criarPrato_QuandoNomeDuplicado_DeveLancarExcecao() {
        PratoRequestDTO request = new PratoRequestDTO("Salada Caesar", "Outra receita", 30.0, 400, true, 1L);

        when(categoriaRepository.existsById(1L)).thenReturn(true);
        when(pratoRepository.existsByNomeIgnoreCase("Salada Caesar")).thenReturn(true);

        assertThrows(RegraNegocioException.class, () -> pratoService.criarPrato(request));
        verify(pratoRepository, never()).save(any(Prato.class));
    }

    @Test
    @DisplayName("Deve atualizar somente o preço com sucesso")
    void atualizarPreco_ComSucesso() {
        when(pratoRepository.findById(1L)).thenReturn(Optional.of(pratoExemplo));
        when(pratoRepository.save(any(Prato.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prato atualizado = pratoService.atualizarPreco(1L, 32.90);

        assertNotNull(atualizado);
        assertEquals(32.90, atualizado.getPreco());
        verify(pratoRepository, times(1)).save(pratoExemplo);
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao atualizar preço com valor menor ou igual a zero")
    void atualizarPreco_ValorInvalido_DeveLancarExcecao() {
        assertThrows(RegraNegocioException.class, () -> pratoService.atualizarPreco(1L, 0.0));
        assertThrows(RegraNegocioException.class, () -> pratoService.atualizarPreco(1L, -5.0));
        verify(pratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar prato existente com sucesso")
    void deletarPrato_ComSucesso() {
        when(pratoRepository.findById(1L)).thenReturn(Optional.of(pratoExemplo));
        doNothing().when(pratoRepository).delete(pratoExemplo);

        assertDoesNotThrow(() -> pratoService.deletarPrato(1L));
        verify(pratoRepository, times(1)).delete(pratoExemplo);
    }
}
