package br.com.raizesdonordeste.api.integration.pagamento;

/** Timeout, erro de rede, HTTP 5xx do provedor ou circuito aberto. */
public class GatewayIndisponivelException extends RuntimeException {
    public GatewayIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
