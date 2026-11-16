package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Produto do catálogo corporativo (definido pela matriz). */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(name = "preco_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoBase;

    /** Produto sazonal (ex.: linha junina) só aparece dentro da vigência do item de cardápio. */
    @Column(nullable = false)
    private boolean sazonal;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Produto() {
    }

    public Produto(String nome, String descricao, String categoria, BigDecimal precoBase, boolean sazonal) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.precoBase = precoBase;
        this.sazonal = sazonal;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public boolean isSazonal() { return sazonal; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
