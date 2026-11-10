package br.com.nutriexpress.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.nutriexpress.demo.exception.RegraNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "cliente_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_pedido_cliente")
    )
    private Cliente cliente;

    private String clienteNome;
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    @Embedded
    private Endereco enderecoEntrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    private Double valorTotal = 0.0;

    public Pedido() {
        this.dataHora = LocalDateTime.now();
        this.status = StatusPedido.PENDENTE;
    }

    public void adicionarItem(ItemPedido item) {
        itens.add(item);
        item.setPedido(this);
        recalcularValorTotal();
    }

    public void recalcularValorTotal() {
        this.valorTotal = itens.stream()
                .mapToDouble(i -> i.getSubtotal() != null ? i.getSubtotal() : 0.0)
                .sum();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Define ou vincula o cliente ao pedido de forma estavel e imutavel.
     * 1. Rejeita cliente nulo.
     * 2. Primeira associacao: vincula this.cliente e inclui na colecao do cliente.
     * 3. Mesma referencia: operacao idempotente.
     * 4. Instancia diferente com o mesmo ID: preserva a referencia original intacta e nao polui a colecao da segunda instancia.
     * 5. Clientes com IDs diferentes ou incompativeis: lanca RegraNegocioException bloqueando transferencia.
     */
    public void setCliente(Cliente novoCliente) {
        if (novoCliente == null) {
            throw new IllegalArgumentException("O cliente do pedido é obrigatório e não pode ser nulo.");
        }

        if (this.cliente == null) {
            this.cliente = novoCliente;
            novoCliente.incluirPedidoInterno(this);
            return;
        }

        if (this.cliente == novoCliente) {
            return;
        }

        if (this.cliente.getId() != null && novoCliente.getId() != null
                && this.cliente.getId().equals(novoCliente.getId())) {
            return;
        }

        throw new RegraNegocioException("Não é permitido transferir a titularidade de um pedido existente.");
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public Endereco getEnderecoEntrega() {
        return enderecoEntrega;
    }

    public void setEnderecoEntrega(Endereco enderecoEntrega) {
        this.enderecoEntrega = enderecoEntrega;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
        if (itens != null) {
            itens.forEach(i -> i.setPedido(this));
        }
        recalcularValorTotal();
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }
}
