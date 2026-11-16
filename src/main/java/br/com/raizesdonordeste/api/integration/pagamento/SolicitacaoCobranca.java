package br.com.raizesdonordeste.api.integration.pagamento;

import br.com.raizesdonordeste.api.domain.MetodoPagamento;

import java.math.BigDecimal;

/** Dados enviados ao serviço externo. A chave de idempotência é reenviada idêntica em toda nova tentativa. */
public record SolicitacaoCobranca(Long pagamentoId, Long pedidoId, BigDecimal valor, MetodoPagamento metodo, String chaveIdempotencia) {
}
