package br.com.raizesdonordeste.api.config;

import br.com.raizesdonordeste.api.service.PagamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tenta de novo, periodicamente, os pagamentos que ficaram sem resposta do provedor. */
@Component
@ConditionalOnProperty(name = "app.pagamento.reprocessamento.habilitado", havingValue = "true", matchIfMissing = true)
public class ReprocessadorPagamentos {

    private static final Logger log = LoggerFactory.getLogger(ReprocessadorPagamentos.class);
    private final PagamentoService pagamentoService;

    public ReprocessadorPagamentos(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @Scheduled(fixedDelayString = "${app.pagamento.reprocessamento.intervalo-ms:30000}", initialDelay = 30000)
    public void executar() {
        int resolvidos = pagamentoService.reprocessarFalhas();
        if (resolvidos > 0) {
            log.info("{} pagamento(s) reprocessado(s) com sucesso", resolvidos);
        }
    }
}
