package br.com.nutriexpress.demo.model;

import java.util.Objects;

import br.com.nutriexpress.demo.exception.RegraNegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    private Integer quantidadeEstoque = 0;

    public Prato() {
    }

    public Prato(Long id, String nome, String descricao, Double preco, Integer calorias, Boolean disponivel, Categoria categoria, Integer quantidadeEstoque) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.calorias = calorias;
        this.disponivel = disponivel;
        this.quantidadeEstoque = quantidadeEstoque != null ? quantidadeEstoque : 0;
        if (categoria != null) {
            this.trocarCategoria(categoria);
        }
    }

    public Prato(Long id, String nome, String descricao, Double preco, Integer calorias, Boolean disponivel, Categoria categoria) {
        this(id, nome, descricao, preco, calorias, disponivel, categoria, 0);
    }

    public Prato(String nome, String descricao, Double preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = 0;
    }

    /**
     * Centraliza a sincronizacao bidirecional entre Prato e Categoria.
     * 1. Rejeita categoria nula.
     * 2. Se ja estiver associado a mesma categoria, garante que esteja na lista interna e retorna sem duplicar.
     * 3. Remove o prato da lista interna da categoria anterior (se houver).
     * 4. Atualiza a referencia 'this.categoria'.
     * 5. Inclui o prato na lista interna da nova categoria.
     */
    public void trocarCategoria(Categoria novaCategoria) {
        Objects.requireNonNull(novaCategoria, "A categoria de um prato não pode ser nula.");
        if (this.categoria == novaCategoria) {
            novaCategoria.incluirPratoInterno(this);
            return;
        }
        if (this.categoria != null) {
            this.categoria.desvincularPratoInterno(this);
        }
        this.categoria = novaCategoria;
        novaCategoria.incluirPratoInterno(this);
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

    public Categoria getCategoria() {
        return categoria;
    }

    /**
     * Define ou altera a categoria do prato garantindo a sincronizacao bidirecional.
     * Delega integralmente a regra para trocarCategoria().
     */
    public void setCategoria(Categoria categoria) {
        this.trocarCategoria(categoria);
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }
}
