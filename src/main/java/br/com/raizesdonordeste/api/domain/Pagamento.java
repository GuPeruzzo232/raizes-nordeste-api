package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Registro local de uma solicitação ao serviço externo de pagamento.
 * O sistema não processa cartão: só solicita, recebe a resposta e registra.
 */
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    /** Identificador devolvido pelo gateway; único para impedir registro em dobro. */
    @Column(name = "transacao_externa", unique = true, length = 80)
    private String transacaoExterna;

    @Column(name = "idempotency_key", unique = true, length = 80)
    private String idempotencyKey;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "ultima_mensagem", length = 300)
    private String ultimaMensagem;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Version
    private Long versao;

    protected Pagamento() {
    }

    public Pagamento(Pedido pedido, BigDecimal valor, MetodoPagamento metodo, String idempotencyKey) {
        this.pedido = pedido;
        this.valor = valor;
        this.metodo = metodo;
        this.idempotencyKey = idempotencyKey;
    }

    public boolean isFinalizado() {
        return status == StatusPagamento.APROVADO || status == StatusPagamento.RECUSADO || status == StatusPagamento.ESTORNADO;
    }

    public void registrarTentativa() {
        this.tentativas++;
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(StatusPagamento novo, String transacaoExterna, String mensagem) {
        this.status = novo;
        if (transacaoExterna != null) {
            this.transacaoExterna = transacaoExterna;
        }
        this.ultimaMensagem = mensagem;
        this.atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public BigDecimal getValor() { return valor; }
    public MetodoPagamento getMetodo() { return metodo; }
    public StatusPagamento getStatus() { return status; }
    public String getTransacaoExterna() { return transacaoExterna; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public int getTentativas() { return tentativas; }
    public String getUltimaMensagem() { return ultimaMensagem; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
