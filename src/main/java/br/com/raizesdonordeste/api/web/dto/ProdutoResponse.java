package br.com.raizesdonordeste.api.web.dto;

import java.math.BigDecimal;

public record ProdutoResponse(Long id, String nome, String descricao, String categoria, BigDecimal precoBase, boolean sazonal, boolean ativo) {
}
