package br.com.nutriexpress.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.nutriexpress.demo.model.Pedido;
import br.com.nutriexpress.demo.model.StatusPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Listar pedidos de um cliente específico
    List<Pedido> findByClienteId(Long clienteId);

    // Filtrar pedidos por status (ex: listar todos EM_PREPARO)
    List<Pedido> findByStatus(StatusPedido status);

    // Filtrar pedidos por período de datas
    List<Pedido> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
}
