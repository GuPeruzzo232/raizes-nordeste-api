package br.com.raizesdonordeste.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoRequest(@NotBlank @Size(min = 5, max = 300) String motivo) {
}
