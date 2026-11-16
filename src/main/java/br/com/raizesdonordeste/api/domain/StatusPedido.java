package br.com.raizesdonordeste.api.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida do pedido. As transições válidas ficam centralizadas aqui
 * para que nenhuma camada consiga "pular" etapas (ex.: entregar sem pagar).
 */
public enum StatusPedido {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    EM_PREPARO,
    PRONTO,
    ENTREGUE,
    CANCELADO;

    public Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> EnumSet.of(PAGO, CANCELADO);
            case PAGO -> EnumSet.of(EM_PREPARO, CANCELADO);
            case EM_PREPARO -> EnumSet.of(PRONTO, CANCELADO);
            case PRONTO -> EnumSet.of(ENTREGUE);
            case ENTREGUE, CANCELADO -> EnumSet.noneOf(StatusPedido.class);
        };
    }

    public boolean podeIrPara(StatusPedido destino) {
        return proximosPermitidos().contains(destino);
    }
}
