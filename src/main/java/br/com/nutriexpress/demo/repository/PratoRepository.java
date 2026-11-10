package br.com.nutriexpress.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.nutriexpress.demo.model.Prato;

public interface PratoRepository extends JpaRepository<Prato, Long> {

    // Listar apenas pratos marcados como disponíveis
    List<Prato> findByDisponivelTrue();

    // Filtrar pratos pertencentes a uma categoria específica navegando pela chave estrangeira
    List<Prato> findByCategoria_Id(Long categoriaId);

    // Buscar pratos pelo nome (busca parcial, ex: "Frango" encontra "Salada com Frango")
    List<Prato> findByNomeContainingIgnoreCase(String nome);

    // Filtrar pratos com calorias até o valor máximo informado
    List<Prato> findByCaloriasLessThanEqual(Integer calorias);

    // Verificar se já existe prato com o mesmo nome
    boolean existsByNomeIgnoreCase(String nome);

    // Verificar se já existe outro prato com o mesmo nome, ignorando o próprio ID
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
