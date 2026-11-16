package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.MetodoPagamento;
import jakarta.validation.constraints.NotNull;

public record SolicitarPagamentoRequest(@NotNull MetodoPagamento metodo) {
}
