package br.com.raizesdonordeste.api.web.dto;

import java.time.Instant;

public record EstoqueResponse(Long unidadeId, Long produtoId, String produto, int quantidade, Instant atualizadoEm) {
}
