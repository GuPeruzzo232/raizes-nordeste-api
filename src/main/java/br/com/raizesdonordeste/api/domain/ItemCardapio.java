package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Liga um produto a uma unidade: é aqui que o cardápio varia por loja
 * (preço local, disponibilidade e janela de vigência para itens sazonais).
 */
@Entity
@Table(name = "item_cardapio",
        uniqueConstraints = @UniqueConstraint(name = "uk_cardapio_unidade_produto", columnNames = {"unidade_id", "produto_id"}))
public class ItemCardapio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(name = "inicio_vigencia")
    private LocalDate inicioVigencia;

    @Column(name = "fim_vigencia")
    private LocalDate fimVigencia;

    protected ItemCardapio() {
    }

    public ItemCardapio(Unidade unidade, Produto produto, BigDecimal preco, LocalDate inicioVigencia, LocalDate fimVigencia) {
        this.unidade = unidade;
        this.produto = produto;
        this.preco = preco;
        this.inicioVigencia = inicioVigencia;
        this.fimVigencia = fimVigencia;
    }

    public boolean vigenteEm(LocalDate data) {
        boolean depoisDoInicio = inicioVigencia == null || !data.isBefore(inicioVigencia);
        boolean antesDoFim = fimVigencia == null || !data.isAfter(fimVigencia);
        return disponivel && produto.isAtivo() && depoisDoInicio && antesDoFim;
    }

    public Long getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Produto getProduto() { return produto; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
    public LocalDate getInicioVigencia() { return inicioVigencia; }
    public void setInicioVigencia(LocalDate inicioVigencia) { this.inicioVigencia = inicioVigencia; }
    public LocalDate getFimVigencia() { return fimVigencia; }
    public void setFimVigencia(LocalDate fimVigencia) { this.fimVigencia = fimVigencia; }
}
