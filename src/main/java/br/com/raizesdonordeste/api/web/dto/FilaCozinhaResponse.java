package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.CanalPedido;
import br.com.raizesdonordeste.api.domain.StatusPedido;

import java.time.Instant;
import java.util.List;

/** Visão da cozinha: só o necessário para preparar. Sem nome, CPF, valores ou dados de pagamento. */
public record FilaCozinhaResponse(Long pedidoId, CanalPedido canal, StatusPedido status, String observacao, Instant criadoEm, List<Item> itens) {

    public record Item(String produto, int quantidade) {
    }
}
