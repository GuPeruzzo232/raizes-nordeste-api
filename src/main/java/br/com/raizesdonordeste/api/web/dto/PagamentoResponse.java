package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.MetodoPagamento;
import br.com.raizesdonordeste.api.domain.StatusPagamento;
import br.com.raizesdonordeste.api.domain.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoResponse(Long id, Long pedidoId, BigDecimal valor, MetodoPagamento metodo, StatusPagamento status,
                                StatusPedido statusPedido, String transacaoExterna, int tentativas, String mensagem,
                                Instant atualizadoEm) {
}
