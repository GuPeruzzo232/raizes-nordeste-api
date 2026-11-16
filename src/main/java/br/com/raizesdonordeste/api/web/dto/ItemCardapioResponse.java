package br.com.raizesdonordeste.api.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemCardapioResponse(Long produtoId, String nome, String descricao, String categoria, BigDecimal preco,
                                   boolean sazonal, LocalDate disponivelAte, boolean emEstoque) {
}
