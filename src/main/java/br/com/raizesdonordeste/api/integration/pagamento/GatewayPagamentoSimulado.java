package br.com.raizesdonordeste.api.integration.pagamento;

import br.com.raizesdonordeste.api.domain.StatusPagamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simulador do provedor de pagamento para desenvolvimento e testes.
 * Assim como um PSP real, respeita a chave de idempotência: a mesma chave
 * devolve sempre a mesma transação, sem cobrar duas vezes.
 */
@Component
public class GatewayPagamentoSimulado implements GatewayPagamento {

    public enum Modo { APROVAR, RECUSAR, INDISPONIVEL }

    private final AtomicReference<Modo> modo;
    private final Map<String, RespostaGateway> processadas = new ConcurrentHashMap<>();
    private final AtomicInteger chamadas = new AtomicInteger();

    public GatewayPagamentoSimulado(@Value("${app.pagamento.simulador.modo:APROVAR}") Modo modoInicial) {
        this.modo = new AtomicReference<>(modoInicial);
    }

    @Override
    public RespostaGateway cobrar(SolicitacaoCobranca s) {
        chamadas.incrementAndGet();
        if (modo.get() == Modo.INDISPONIVEL) {
            throw new GatewayIndisponivelException("Provedor de pagamento não respondeu (timeout simulado)");
        }
        return processadas.computeIfAbsent(s.chaveIdempotencia(), k -> modo.get() == Modo.APROVAR
                ? new RespostaGateway(StatusPagamento.APROVADO, "SIM-" + UUID.randomUUID(), "Pagamento aprovado")
                : new RespostaGateway(StatusPagamento.RECUSADO, "SIM-" + UUID.randomUUID(), "Pagamento recusado pelo emissor"));
    }

    public void definirModo(Modo novo) {
        modo.set(novo);
    }

    public Modo getModo() {
        return modo.get();
    }

    public int totalChamadas() {
        return chamadas.get();
    }

    public void limpar() {
        processadas.clear();
        chamadas.set(0);
        modo.set(Modo.APROVAR);
    }
}
