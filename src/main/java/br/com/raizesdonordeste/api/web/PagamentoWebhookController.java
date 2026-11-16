package br.com.raizesdonordeste.api.web;

import br.com.raizesdonordeste.api.service.PagamentoService;
import br.com.raizesdonordeste.api.web.dto.WebhookPagamentoRequest;
import br.com.raizesdonordeste.api.web.dto.WebhookResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamentos")
@Tag(name = "Pagamentos")
public class PagamentoWebhookController {

    private final PagamentoService service;

    public PagamentoWebhookController(PagamentoService service) {
        this.service = service;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Notificação do provedor de pagamento (idempotente por eventoId; exige X-Webhook-Secret)")
    public WebhookResposta webhook(@RequestHeader(name = "X-Webhook-Secret", required = false) String segredo,
                                   @Valid @RequestBody WebhookPagamentoRequest req) {
        return service.receberWebhook(segredo, req);
    }
}
