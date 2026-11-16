package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Saldo de um produto em uma unidade específica. */
@Entity
@Table(name = "estoque",
        uniqueConstraints = @UniqueConstraint(name = "uk_estoque_unidade_produto", columnNames = {"unidade_id", "produto_id"}))
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Estoque() {
    }

    public Estoque(Unidade unidade, Produto produto, int quantidade) {
        this.unidade = unidade;
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Long getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void definirQuantidade(int quantidade) {
        this.quantidade = quantidade;
        this.atualizadoEm = Instant.now();
    }
}
