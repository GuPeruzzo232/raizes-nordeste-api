package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    /** Cliente identificado (opcional: pedido de balcão/totem pode ser anônimo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    /** Quem registrou o pedido (o próprio cliente no app ou o atendente no balcão). */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    private Usuario criadoPor;

    /** Canal de origem: app, totem, balcão, pick-up ou web. Obrigatório. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalPedido canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 300)
    private String observacao;

    /** Chave enviada pelo cliente HTTP para evitar pedido duplicado em reenvio. */
    @Column(name = "idempotency_key", unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "motivo_cancelamento", length = 300)
    private String motivoCancelamento;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    /** Controle de concorrência otimista (dois atendentes alterando o mesmo pedido). */
    @Version
    private Long versao;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Unidade unidade, Usuario cliente, Usuario criadoPor, CanalPedido canal, String observacao, String idempotencyKey) {
        this.unidade = unidade;
        this.cliente = cliente;
        this.criadoPor = criadoPor;
        this.canal = canal;
        this.observacao = observacao;
        this.idempotencyKey = idempotencyKey;
    }

    public void adicionarItem(Produto produto, int quantidade, BigDecimal precoUnitario) {
        itens.add(new ItemPedido(this, produto, quantidade, precoUnitario));
        recalcular();
    }

    public void aplicarDesconto(BigDecimal valor) {
        this.desconto = valor.setScale(2, RoundingMode.HALF_UP);
        recalcular();
    }

    private void recalcular() {
        this.subtotal = itens.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = subtotal.subtract(desconto).max(BigDecimal.ZERO);
        this.atualizadoEm = Instant.now();
    }

    public void alterarStatus(StatusPedido novo) {
        this.status = novo;
        this.atualizadoEm = Instant.now();
    }

    public void cancelar(String motivo) {
        this.motivoCancelamento = motivo;
        alterarStatus(StatusPedido.CANCELADO);
    }

    public Long getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Usuario getCliente() { return cliente; }
    public Usuario getCriadoPor() { return criadoPor; }
    public CanalPedido getCanal() { return canal; }
    public StatusPedido getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDesconto() { return desconto; }
    public BigDecimal getTotal() { return total; }
    public String getObservacao() { return observacao; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public List<ItemPedido> getItens() { return itens; }
}
