package br.com.nutriexpress.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "prato_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_item_pedido_prato")
    )
    private Prato prato;

    private String pratoNome;
    private Integer quantidade;
    private Double precoUnitario;
    private Double subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @JsonIgnore
    private Pedido pedido;

    public ItemPedido() {
    }

    public ItemPedido(Prato prato, Integer quantidade, Double precoUnitario) {
        if (prato == null) {
            throw new IllegalArgumentException("O prato associado ao item é obrigatório e não pode ser nulo.");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade do item deve ser obrigatória e maior que zero.");
        }
        if (precoUnitario == null || precoUnitario <= 0) {
            throw new IllegalArgumentException("O preço unitário histórico é obrigatório e deve ser maior que zero.");
        }

        this.prato = prato;
        this.pratoNome = prato.getNome();
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = precoUnitario * quantidade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prato getPrato() {
        return prato;
    }

    /**
     * Define ou vincula o prato ao item.
     * Permite a primeira atribuição (inclusive pelo Hibernate na hidratação),
     * mas bloqueia categoricamente a troca de prato de um item já criado.
     */
    public void setPrato(Prato novoPrato) {
        if (novoPrato == null) {
            throw new IllegalArgumentException("O prato do item não pode ser nulo.");
        }
        if (this.prato == null) {
            this.prato = novoPrato;
            return;
        }
        if (this.prato == novoPrato ||
            (this.prato.getId() != null && novoPrato.getId() != null && this.prato.getId().equals(novoPrato.getId()))) {
            return;
        }
        throw new RegraNegocioException("Não é permitido alterar o prato de um item de pedido já criado.");
    }

    public String getPratoNome() {
        return pratoNome;
    }

    public void setPratoNome(String pratoNome) {
        this.pratoNome = pratoNome;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade do item deve ser obrigatória e maior que zero.");
        }
        this.quantidade = quantidade;
        if (this.precoUnitario != null) {
            this.subtotal = this.precoUnitario * quantidade;
        }
    }

    public Double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(Double precoUnitario) {
        if (precoUnitario == null || precoUnitario <= 0) {
            throw new IllegalArgumentException("O preço unitário histórico é obrigatório e deve ser maior que zero.");
        }
        this.precoUnitario = precoUnitario;
        if (this.quantidade != null) {
            this.subtotal = precoUnitario * this.quantidade;
        }
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }
}
