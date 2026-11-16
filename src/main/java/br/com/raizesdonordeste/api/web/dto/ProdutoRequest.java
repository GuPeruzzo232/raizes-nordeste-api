package br.com.raizesdonordeste.api.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(@NotBlank @Size(max = 120) String nome,
                             @Size(max = 500) String descricao,
                             @NotBlank @Size(max = 60) String categoria,
                             @NotNull @DecimalMin("0.01") BigDecimal precoBase,
                             boolean sazonal) {
}
