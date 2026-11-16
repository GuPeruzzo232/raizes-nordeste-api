package br.com.raizesdonordeste.api.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Desconto em percentual sobre o subtotal. O teto por perfil é validado no serviço. */
public record DescontoRequest(@NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal percentual,
                              @NotBlank @Size(min = 5, max = 300) String motivo) {
}
