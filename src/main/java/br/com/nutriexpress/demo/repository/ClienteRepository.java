package br.com.nutriexpress.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.nutriexpress.demo.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Listar apenas clientes ativos (Soft Delete)
    List<Cliente> findByAtivoTrue();

    // Buscar por CPF ou E-mail
    Optional<Cliente> findByCpf(String cpf);
    Optional<Cliente> findByEmail(String email);

    // Validações de duplicidade
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);

    // Busca por parte do nome
    List<Cliente> findByNomeContainingIgnoreCase(String nome);
}
