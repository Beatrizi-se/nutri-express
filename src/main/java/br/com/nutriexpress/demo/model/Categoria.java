package br.com.nutriexpress.demo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;

    @OneToMany(mappedBy = "categoria")
    private List<Prato> pratos = new ArrayList<>();

    public Categoria() {
    }

    public Categoria(Long id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
    }

    /**
     * Adiciona um prato a esta categoria delegando a regra para o prato.
     * Centraliza a sincronizacao no metodo Prato.trocarCategoria().
     */
    public void adicionarPrato(Prato prato) {
        if (prato == null) {
            throw new IllegalArgumentException("O prato a ser adicionado não pode ser nulo.");
        }
        prato.trocarCategoria(this);
    }

    /**
     * Metodo interno para inclusao direta na lista privada.
     * Deve ser invocado exclusivamente pelo fluxo de sincronizacao de Prato.
     */
    void incluirPratoInterno(Prato prato) {
        if (prato != null && !this.pratos.contains(prato)) {
            this.pratos.add(prato);
        }
    }

    /**
     * Metodo interno para desvinculacao direta da lista privada.
     * Deve ser invocado exclusivamente pelo fluxo de sincronizacao de Prato.
     */
    void desvincularPratoInterno(Prato prato) {
        if (prato != null) {
            this.pratos.remove(prato);
        }
    }

    public List<Prato> getPratos() {
        return Collections.unmodifiableList(pratos);
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
}
