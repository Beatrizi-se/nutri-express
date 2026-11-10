package br.com.nutriexpress.demo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.model.Cliente;
import br.com.nutriexpress.demo.model.Endereco;
import br.com.nutriexpress.demo.model.ItemPedido;
import br.com.nutriexpress.demo.model.Pedido;
import br.com.nutriexpress.demo.model.Prato;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IntegridadeReferencialPratoTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    @DisplayName("A constraint de chave estrangeira deve impedir a exclusão física de prato associado a pedido")
    void deletarPrato_ComPedidosAssociados_DeveLancarDataIntegrityViolationException() {
        // 1. Persistir Categoria
        Categoria categoria = new Categoria(null, "Saladas", "Saladas leves");
        categoria = categoriaRepository.saveAndFlush(categoria);

        // 2. Persistir Prato associado à Categoria
        Prato prato = new Prato(null, "Salada Especial", "Salada completa", 30.0, 250, true, categoria, 20);
        prato = pratoRepository.saveAndFlush(prato);

        // 3. Persistir Cliente
        Endereco endereco = new Endereco("Rua das Flores", "123", null, "Centro", "São Paulo", "01001-000");
        Cliente cliente = new Cliente(null, "Roberto Teste", "roberto@teste.com", "99988877766", "11988887777", null, true, endereco);
        cliente = clienteRepository.saveAndFlush(cliente);

        // 4. Criar e Persistir Pedido com ItemPedido associado ao Prato
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setClienteNome(cliente.getNome());
        pedido.setEnderecoEntrega(cliente.getEndereco());

        ItemPedido item = new ItemPedido(prato, 2, 30.0);
        pedido.adicionarItem(item);

        pedido = pedidoRepository.saveAndFlush(pedido);

        Long pratoId = prato.getId();
        Long pedidoId = pedido.getId();

        // 5. Tenta deletar o prato fisicamente em uma transação dedicada com flush()
        // O banco H2 intercepta via constraint fk_item_pedido_prato e dispara DataIntegrityViolationException
        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                pratoRepository.deleteById(pratoId);
                pratoRepository.flush();
                return null;
            });
        });

        // 6. Comprova em uma transação de leitura que o Prato e o Pedido (com itens) continuam preservados
        transactionTemplate.execute(status -> {
            Prato pratoAposTentativa = pratoRepository.findById(pratoId).orElse(null);
            assertNotNull(pratoAposTentativa, "O prato deve continuar existindo no banco de dados");
            assertEquals("Salada Especial", pratoAposTentativa.getNome());

            Pedido pedidoAposTentativa = pedidoRepository.findById(pedidoId).orElse(null);
            assertNotNull(pedidoAposTentativa, "O pedido deve continuar existindo no banco de dados");
            assertEquals(1, pedidoAposTentativa.getItens().size());
            assertEquals("Salada Especial", pedidoAposTentativa.getItens().get(0).getPratoNome());
            assertEquals(30.0, pedidoAposTentativa.getItens().get(0).getPrecoUnitario());
            return null;
        });
    }
}
