package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.CanalPedido;

import java.math.BigDecimal;

public record VendaResumoResponse(Long unidadeId, String unidade, CanalPedido canal, long quantidadePedidos, BigDecimal faturamento) {
}
