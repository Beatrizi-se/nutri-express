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
import br.com.nutriexpress.demo.model.Categoria;
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
    private Categoria categoriaExemplo;

    @BeforeEach
    void setUp() {
        categoriaExemplo = new Categoria(1L, "Saladas", "Pratos saudáveis");
        pratoExemplo = new Prato(1L, "Salada Caesar", "Alface, frango e molho especial", 28.50, 350, true, categoriaExemplo, 15);
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
        PratoRequestDTO request = new PratoRequestDTO("Wrap de Frango", "Wrap integral", 22.0, 310, true, 1L, 20);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaExemplo));
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
        assertEquals(20, criado.getQuantidadeEstoque());
        assertEquals(1L, criado.getCategoria().getId());
        verify(categoriaRepository, times(1)).findById(1L);
        verify(pratoRepository, times(1)).save(any(Prato.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar criar prato para categoria inexistente")
    void criarPrato_QuandoCategoriaNaoExiste_DeveLancarExcecao() {
        PratoRequestDTO request = new PratoRequestDTO("Wrap de Frango", "Wrap integral", 22.0, 310, true, 999L, 10);

        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pratoService.criarPrato(request));
        verify(pratoRepository, never()).save(any(Prato.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar cadastrar prato com nome duplicado")
    void criarPrato_QuandoNomeDuplicado_DeveLancarExcecao() {
        PratoRequestDTO request = new PratoRequestDTO("Salada Caesar", "Outra receita", 30.0, 400, true, 1L, 10);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaExemplo));
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
    @DisplayName("Deve atualizar a quantidade em estoque com sucesso")
    void atualizarEstoque_ComSucesso() {
        when(pratoRepository.findById(1L)).thenReturn(Optional.of(pratoExemplo));
        when(pratoRepository.save(any(Prato.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prato atualizado = pratoService.atualizarEstoque(1L, 50);

        assertNotNull(atualizado);
        assertEquals(50, atualizado.getQuantidadeEstoque());
        assertTrue(atualizado.getDisponivel());
        verify(pratoRepository, times(1)).save(pratoExemplo);
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao atualizar estoque com valor negativo")
    void atualizarEstoque_ValorNegativo_DeveLancarExcecao() {
        assertThrows(RegraNegocioException.class, () -> pratoService.atualizarEstoque(1L, -5));
        verify(pratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar prato e trocar categoria com sucesso e consistência bidirecional")
    void atualizarPrato_TrocaCategoria_ComSucesso() {
        // Categoria inicial A já contendo o prato
        Categoria categoriaInicial = categoriaExemplo;
        assertTrue(categoriaInicial.getPratos().contains(pratoExemplo));

        // Categoria nova B
        Categoria novaCategoria = new Categoria(2L, "Pratos Quentes", "Grelhados e assados");
        PratoRequestDTO request = new PratoRequestDTO("Salada Caesar Modificada", "Com croutons integrais", 35.0, 380, true, 2L, 25);

        when(pratoRepository.findById(1L)).thenReturn(Optional.of(pratoExemplo));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(novaCategoria));
        when(pratoRepository.existsByNomeIgnoreCaseAndIdNot("Salada Caesar Modificada", 1L)).thenReturn(false);
        when(pratoRepository.save(any(Prato.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prato atualizado = pratoService.atualizarPrato(1L, request);

        assertNotNull(atualizado);
        assertEquals("Salada Caesar Modificada", atualizado.getNome());
        assertEquals(2L, atualizado.getCategoria().getId());

        // Comprova a remoção da categoria inicial A e inclusão na nova categoria B
        assertFalse(categoriaInicial.getPratos().contains(atualizado));
        assertTrue(novaCategoria.getPratos().contains(atualizado));

        verify(categoriaRepository, times(1)).findById(2L);
        verify(pratoRepository, times(1)).save(pratoExemplo);
    }

    @Test
    @DisplayName("Deve manter a sincronização bidirecional completa, evitar duplicatas e desvincular da anterior")
    void sincronizacaoBidirecional_DeveGarantirConsistencia() {
        Categoria catA = new Categoria(10L, "Sobremesas", "Doces fit");
        Categoria catB = new Categoria(20L, "Lanches", "Lanches saudáveis");

        // 1. Associação inicial através do construtor
        Prato prato = new Prato(100L, "Mousse Fit", "Cacau 70%", 15.0, 150, true, catA);
        assertEquals(catA, prato.getCategoria());
        assertTrue(catA.getPratos().contains(prato));
        assertEquals(1, catA.getPratos().size());

        // 2. Ausência de duplicatas ao adicionar novamente à mesma categoria
        catA.adicionarPrato(prato);
        assertEquals(1, catA.getPratos().size());

        prato.trocarCategoria(catA);
        assertEquals(1, catA.getPratos().size());

        // 3. Transferência da categoria A para a B
        prato.trocarCategoria(catB);

        // 4. Remoção do prato da lista de A e inclusão na lista de B
        assertFalse(catA.getPratos().contains(prato));
        assertEquals(0, catA.getPratos().size());

        assertTrue(catB.getPratos().contains(prato));
        assertEquals(1, catB.getPratos().size());
        assertEquals(catB, prato.getCategoria());

        // 5. Associação via Categoria.adicionarPrato() para um prato que já pertencia a outra categoria
        catA.adicionarPrato(prato);
        assertTrue(catA.getPratos().contains(prato));
        assertEquals(1, catA.getPratos().size());
        assertFalse(catB.getPratos().contains(prato));
        assertEquals(0, catB.getPratos().size());
        assertEquals(catA, prato.getCategoria());

        // 6. Rejeição de categoria nula
        assertThrows(NullPointerException.class, () -> prato.trocarCategoria(null));
        assertThrows(NullPointerException.class, () -> prato.setCategoria(null));
        assertThrows(IllegalArgumentException.class, () -> catA.adicionarPrato(null));

        // 7. Impossibilidade de modificar diretamente a coleção retornada por getPratos()
        List<Prato> listaPratos = catA.getPratos();
        assertThrows(UnsupportedOperationException.class, () -> listaPratos.add(new Prato()));
        assertThrows(UnsupportedOperationException.class, () -> listaPratos.remove(prato));
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
