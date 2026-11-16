package br.com.raizesdonordeste.api.integration.pagamento;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Disjuntor (circuit breaker) mínimo: após N falhas seguidas o circuito abre e as chamadas
 * falham na hora, sem esperar timeout, protegendo a API em pico quando o provedor cai.
 * Após o tempo de espera, uma chamada de teste (meio-aberto) decide se fecha de novo.
 */
public class CircuitBreakerSimples {

    public enum Estado { FECHADO, ABERTO, MEIO_ABERTO }

    private final int limiteFalhas;
    private final Duration tempoAberto;
    private final Clock clock;

    private Estado estado = Estado.FECHADO;
    private int falhasSeguidas;
    private Instant abertoEm;

    public CircuitBreakerSimples(int limiteFalhas, Duration tempoAberto, Clock clock) {
        this.limiteFalhas = limiteFalhas;
        this.tempoAberto = tempoAberto;
        this.clock = clock;
    }

    public synchronized <T> T executar(Supplier<T> chamada) {
        if (estado == Estado.ABERTO) {
            if (Instant.now(clock).isAfter(abertoEm.plus(tempoAberto))) {
                estado = Estado.MEIO_ABERTO;
            } else {
                throw new GatewayIndisponivelException("Circuito aberto: provedor de pagamento indisponível, tentativa adiada");
            }
        }
        try {
            T resultado = chamada.get();
            estado = Estado.FECHADO;
            falhasSeguidas = 0;
            return resultado;
        } catch (GatewayIndisponivelException e) {
            falhasSeguidas++;
            if (estado == Estado.MEIO_ABERTO || falhasSeguidas >= limiteFalhas) {
                estado = Estado.ABERTO;
                abertoEm = Instant.now(clock);
            }
            throw e;
        }
    }

    public synchronized Estado getEstado() {
        return estado;
    }

    public synchronized void reiniciar() {
        estado = Estado.FECHADO;
        falhasSeguidas = 0;
        abertoEm = null;
    }
}
