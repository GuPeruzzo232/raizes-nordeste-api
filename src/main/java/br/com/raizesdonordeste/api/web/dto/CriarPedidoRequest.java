package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.CanalPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Todos os canais (app, totem, balcão, pick-up, web) usam este mesmo contrato.
 * O campo {@code canal} é obrigatório e fica gravado no pedido.
 */
public record CriarPedidoRequest(
        @NotNull Long unidadeId,
        @NotNull CanalPedido canal,
        Long clienteId,
        @Size(max = 300) String observacao,
        @NotEmpty @Size(max = 30) List<@Valid Item> itens) {

    public record Item(@NotNull Long produtoId, @NotNull @Min(1) @Max(50) Integer quantidade) {
    }
}
