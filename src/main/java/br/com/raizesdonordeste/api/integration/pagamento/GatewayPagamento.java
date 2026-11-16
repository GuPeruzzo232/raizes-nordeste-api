package br.com.raizesdonordeste.api.integration.pagamento;

/**
 * Porta de saída para o provedor de pagamento (PSP). A API não guarda dados de cartão:
 * apenas solicita a cobrança e registra a resposta. Trocar de provedor = nova implementação.
 */
public interface GatewayPagamento {

    RespostaGateway cobrar(SolicitacaoCobranca solicitacao) throws GatewayIndisponivelException;
}
