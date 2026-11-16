package br.com.raizesdonordeste.api.exception;

/** Conflito com o estado atual do recurso (HTTP 409), ex.: estoque insuficiente, pedido já pago. */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
