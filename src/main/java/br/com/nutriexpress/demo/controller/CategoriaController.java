package br.com.nutriexpress.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.service.CategoriaService;
import br.com.nutriexpress.demo.dto.categoria.CategoriaRequestDTO;
import br.com.nutriexpress.demo.dto.categoria.CategoriaResponseDTO;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    // Injeção de dependência via construtor (recomendado pelo Spring)
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listarCategorias() {
        List<CategoriaResponseDTO> categorias = categoriaService.listarCategorias()
            .stream()
            .map(CategoriaResponseDTO::new)
            .toList();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> getCategoriaById(@PathVariable Long id) {
        CategoriaResponseDTO categoria = new CategoriaResponseDTO(categoriaService.getCategoriaById(id));
        return ResponseEntity.ok(categoria);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> criarCategoria(@Valid @RequestBody CategoriaRequestDTO cat) {
        Categoria categoriaCriada = categoriaService.criarCategoria(cat);
        CategoriaResponseDTO categoriaResponse = new CategoriaResponseDTO(categoriaCriada);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequestDTO cat) {
        Categoria categoriaAtualizada = categoriaService.atualizarCategoria(id, cat);
        CategoriaResponseDTO categoriaResponse = new CategoriaResponseDTO(categoriaAtualizada);
        return ResponseEntity.ok(categoriaResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable Long id) {
        categoriaService.deletarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
