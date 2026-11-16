package br.com.raizesdonordeste.api.exception;

/** O usuário está autenticado, mas o recurso não pertence ao seu escopo (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
