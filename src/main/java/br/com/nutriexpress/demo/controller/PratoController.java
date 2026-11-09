package br.com.nutriexpress.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.nutriexpress.demo.dto.prato.PratoRequestDTO;
import br.com.nutriexpress.demo.dto.prato.PratoResponseDTO;
import br.com.nutriexpress.demo.model.Prato;
import br.com.nutriexpress.demo.service.PratoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pratos")
public class PratoController {

    private final PratoService pratoService;

    // Injeção de dependência via construtor
    public PratoController(PratoService pratoService) {
        this.pratoService = pratoService;
    }

    /**
     * Listagem com suporte a filtros dinâmicos via Query Parameters:
     * - /api/v1/pratos
     * - /api/v1/pratos?categoriaId=1
     * - /api/v1/pratos?nome=Frango
     * - /api/v1/pratos?caloriasMaximas=500
     * - /api/v1/pratos?apenasDisponiveis=true
     */
    @GetMapping
    public ResponseEntity<List<PratoResponseDTO>> listarPratos(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Integer caloriasMaximas,
            @RequestParam(required = false) Boolean apenasDisponiveis) {

        List<Prato> pratos;

        if (categoriaId != null) {
            pratos = pratoService.buscarPorCategoria(categoriaId);
        } else if (nome != null && !nome.isBlank()) {
            pratos = pratoService.buscarPorNome(nome);
        } else if (caloriasMaximas != null) {
            pratos = pratoService.buscarPorCaloriasMaximas(caloriasMaximas);
        } else if (Boolean.TRUE.equals(apenasDisponiveis)) {
            pratos = pratoService.listarDisponiveis();
        } else {
            pratos = pratoService.listarTodos();
        }

        List<PratoResponseDTO> dtos = pratos.stream()
                .map(PratoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PratoResponseDTO> buscarPorId(@PathVariable Long id) {
        Prato prato = pratoService.buscarPorId(id);
        return ResponseEntity.ok(new PratoResponseDTO(prato));
    }

    @PostMapping
    public ResponseEntity<PratoResponseDTO> criarPrato(@Valid @RequestBody PratoRequestDTO dto) {
        Prato pratoCriado = pratoService.criarPrato(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PratoResponseDTO(pratoCriado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PratoResponseDTO> atualizarPrato(
            @PathVariable Long id,
            @Valid @RequestBody PratoRequestDTO dto) {
        Prato pratoAtualizado = pratoService.atualizarPrato(id, dto);
        return ResponseEntity.ok(new PratoResponseDTO(pratoAtualizado));
    }

    @PatchMapping("/{id}/preco")
    public ResponseEntity<PratoResponseDTO> atualizarPreco(
            @PathVariable Long id,
            @RequestParam Double preco) {
        Prato pratoAtualizado = pratoService.atualizarPreco(id, preco);
        return ResponseEntity.ok(new PratoResponseDTO(pratoAtualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPrato(@PathVariable Long id) {
        pratoService.deletarPrato(id);
        return ResponseEntity.noContent().build();
    }
}
