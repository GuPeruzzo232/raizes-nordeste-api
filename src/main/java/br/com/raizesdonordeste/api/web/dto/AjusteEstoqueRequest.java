package br.com.raizesdonordeste.api.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ajuste manual de estoque (inventário, perda, recebimento). Exige motivo para a auditoria. */
public record AjusteEstoqueRequest(@NotNull @Min(0) Integer quantidade, @NotBlank @Size(max = 200) String motivo) {
}
