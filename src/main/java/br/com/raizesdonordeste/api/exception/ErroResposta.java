package br.com.raizesdonordeste.api.exception;

import java.time.Instant;
import java.util.List;

/** Formato padrão de erro devolvido pela API. */
public record ErroResposta(Instant timestamp, int status, String erro, String mensagem, String caminho, List<String> detalhes) {
}
