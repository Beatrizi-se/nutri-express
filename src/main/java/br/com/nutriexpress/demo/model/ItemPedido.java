package br.com.nutriexpress.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    private Long pratoId;
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

    public ItemPedido(Long pratoId, String pratoNome, Integer quantidade, Double precoUnitario) {
        this.pratoId = pratoId;
        this.pratoNome = pratoNome;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = precoUnitario != null && quantidade != null ? precoUnitario * quantidade : 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPratoId() {
        return pratoId;
    }

    public void setPratoId(Long pratoId) {
        this.pratoId = pratoId;
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
        this.quantidade = quantidade;
        if (this.precoUnitario != null && quantidade != null) {
            this.subtotal = this.precoUnitario * quantidade;
        }
    }

    public Double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(Double precoUnitario) {
        this.precoUnitario = precoUnitario;
        if (precoUnitario != null && this.quantidade != null) {
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
