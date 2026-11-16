package br.com.raizesdonordeste.api.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemCardapioRequest(@NotNull Long produtoId,
                                  @NotNull @DecimalMin("0.01") BigDecimal preco,
                                  Boolean disponivel,
                                  LocalDate inicioVigencia,
                                  LocalDate fimVigencia) {
}
