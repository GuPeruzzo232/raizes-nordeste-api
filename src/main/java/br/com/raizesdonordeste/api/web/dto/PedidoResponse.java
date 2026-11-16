package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.CanalPedido;
import br.com.raizesdonordeste.api.domain.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(Long id, Long unidadeId, String unidade, Long clienteId, CanalPedido canal, StatusPedido status,
                             BigDecimal subtotal, BigDecimal desconto, BigDecimal total, String observacao,
                             String motivoCancelamento, Instant criadoEm, Instant atualizadoEm, List<Item> itens) {

    public record Item(Long produtoId, String produto, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }
}
