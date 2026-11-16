package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.FinalidadeConsentimento;
import jakarta.validation.constraints.NotNull;

public record ConsentimentoRequest(@NotNull FinalidadeConsentimento finalidade, @NotNull Boolean concedido, String canalOrigem) {
}
