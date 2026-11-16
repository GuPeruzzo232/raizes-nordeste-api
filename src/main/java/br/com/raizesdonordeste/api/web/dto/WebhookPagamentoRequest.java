package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.StatusPagamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Notificação assíncrona enviada pelo serviço de pagamento. */
public record WebhookPagamentoRequest(@NotBlank String eventoId,
                                      @NotNull Long pagamentoId,
                                      @NotBlank String transacaoExterna,
                                      @NotNull StatusPagamento status,
                                      String mensagem) {
}
