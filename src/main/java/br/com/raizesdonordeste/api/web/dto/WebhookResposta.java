package br.com.raizesdonordeste.api.web.dto;

public record WebhookResposta(String eventoId, boolean processado, String observacao) {
}
