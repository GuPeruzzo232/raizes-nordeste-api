package br.com.raizesdonordeste.api.integration.pagamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/** Envolve o gateway com o disjuntor. Os serviços só falam com esta classe. */
@Component
public class ClientePagamentoResiliente {

    private final GatewayPagamento gateway;
    private final CircuitBreakerSimples disjuntor;

    public ClientePagamentoResiliente(GatewayPagamento gateway, Clock clock,
                                      @Value("${app.pagamento.disjuntor.limite-falhas:3}") int limiteFalhas,
                                      @Value("${app.pagamento.disjuntor.segundos-aberto:30}") long segundosAberto) {
        this.gateway = gateway;
        this.disjuntor = new CircuitBreakerSimples(limiteFalhas, Duration.ofSeconds(segundosAberto), clock);
    }

    public RespostaGateway cobrar(SolicitacaoCobranca solicitacao) {
        return disjuntor.executar(() -> gateway.cobrar(solicitacao));
    }

    public CircuitBreakerSimples getDisjuntor() {
        return disjuntor;
    }
}
