package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(@NotNull StatusPedido status) {
}
