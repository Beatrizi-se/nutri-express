package br.com.nutriexpress.demo.model;

import br.com.nutriexpress.demo.exception.RegraNegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Prato {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;
    private Double preco;
    private Integer calorias;
    private Boolean disponivel;
    private Long categoriaId;
    private Integer quantidadeEstoque = 0;

    public Prato() {
    }

    public Prato(Long id, String nome, String descricao, Double preco, Integer calorias, Boolean disponivel, Long categoriaId, Integer quantidadeEstoque) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.calorias = calorias;
        this.disponivel = disponivel;
        this.categoriaId = categoriaId;
        this.quantidadeEstoque = quantidadeEstoque != null ? quantidadeEstoque : 0;
    }

    public Prato(Long id, String nome, String descricao, Double preco, Integer calorias, Boolean disponivel, Long categoriaId) {
        this(id, nome, descricao, preco, calorias, disponivel, categoriaId, 0);
    }

    public Prato(String nome, String descricao, Double preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = 0;
    }

    public void decrementarEstoque(int qtd) {
        if (this.quantidadeEstoque == null || this.quantidadeEstoque < qtd) {
            throw new RegraNegocioException("Estoque insuficiente para o prato: " + this.nome);
        }
        this.quantidadeEstoque -= qtd;
        if (this.quantidadeEstoque == 0) {
            this.disponivel = false;
        }
    }

    public void incrementarEstoque(int qtd) {
        if (this.quantidadeEstoque == null) {
            this.quantidadeEstoque = 0;
        }
        this.quantidadeEstoque += qtd;
        if (this.quantidadeEstoque > 0) {
            this.disponivel = true;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }

    public Integer getCalorias() {
        return calorias;
    }

    public void setCalorias(Integer calorias) {
        this.calorias = calorias;
    }

    public Boolean getDisponivel() {
        return disponivel;
    }

    public void setDisponivel(Boolean disponivel) {
        this.disponivel = disponivel;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }
}
