package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Cada notificação (webhook) do gateway tem um id. Guardá-lo com restrição UNIQUE
 * garante que uma resposta repetida seja reconhecida e ignorada (idempotência).
 */
@Entity
@Table(name = "evento_pagamento_recebido")
public class EventoPagamentoRecebido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, unique = true, length = 80)
    private String eventoId;

    @Column(name = "pagamento_id", nullable = false)
    private Long pagamentoId;

    @Column(name = "status_recebido", nullable = false, length = 20)
    private String statusRecebido;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm = Instant.now();

    protected EventoPagamentoRecebido() {
    }

    public EventoPagamentoRecebido(String eventoId, Long pagamentoId, String statusRecebido) {
        this.eventoId = eventoId;
        this.pagamentoId = pagamentoId;
        this.statusRecebido = statusRecebido;
    }

    public Long getId() { return id; }
    public String getEventoId() { return eventoId; }
    public Long getPagamentoId() { return pagamentoId; }
    public String getStatusRecebido() { return statusRecebido; }
    public Instant getRecebidoEm() { return recebidoEm; }
}
