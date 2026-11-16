package br.com.raizesdonordeste.api.integration.pagamento;

import br.com.raizesdonordeste.api.domain.StatusPagamento;

public record RespostaGateway(StatusPagamento status, String transacaoId, String mensagem) {
}
