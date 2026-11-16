package br.com.raizesdonordeste.api.web;

import br.com.raizesdonordeste.api.domain.CanalPedido;
import br.com.raizesdonordeste.api.domain.StatusPedido;
import br.com.raizesdonordeste.api.service.PagamentoService;
import br.com.raizesdonordeste.api.service.PedidoService;
import br.com.raizesdonordeste.api.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos")
public class PedidoController {

    private final PedidoService pedidos;
    private final PagamentoService pagamentos;

    public PedidoController(PedidoService pedidos, PagamentoService pagamentos) {
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE')")
    @Operation(summary = "Cria pedido (qualquer canal). Header Idempotency-Key evita duplicidade em reenvio.")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest req,
                                                @RequestHeader(name = "Idempotency-Key", required = false) String chave) {
        var r = pedidos.criar(req, chave);
        if (!r.criado()) {
            return ResponseEntity.ok(r.pedido());
        }
        return ResponseEntity.created(URI.create("/api/pedidos/" + r.pedido().id())).body(r.pedido());
    }

    @GetMapping
    @Operation(summary = "Lista pedidos no escopo do perfil (cliente: os seus; loja: da unidade; matriz: todos)")
    public PaginaResponse<PedidoResponse> listar(@RequestParam(required = false) Long unidadeId,
                                                 @RequestParam(required = false) StatusPedido status,
                                                 @RequestParam(required = false) CanalPedido canal,
                                                 @RequestParam(defaultValue = "0") int pagina,
                                                 @RequestParam(defaultValue = "20") int tamanho) {
        return pedidos.listar(unidadeId, status, canal, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id) {
        return pedidos.buscar(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ATENDENTE','COZINHA','GERENTE')")
    @Operation(summary = "Avança o status operacional (EM_PREPARO, PRONTO, ENTREGUE)")
    public PedidoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusRequest req) {
        return pedidos.alterarStatus(id, req.status());
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','MATRIZ')")
    @Operation(summary = "Cancela o pedido, devolve estoque e registra auditoria")
    public PedidoResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelamentoRequest req) {
        return pedidos.cancelar(id, req.motivo());
    }

    @PostMapping("/{id}/desconto")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE','MATRIZ')")
    @Operation(summary = "Aplica desconto percentual (limite por perfil, auditado)")
    public PedidoResponse desconto(@PathVariable Long id, @Valid @RequestBody DescontoRequest req) {
        return pedidos.aplicarDesconto(id, req);
    }

    @PostMapping("/{id}/pagamentos")
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE')")
    @Operation(summary = "Solicita o pagamento ao provedor externo. 201 com resposta; 202 se o provedor não respondeu.")
    public ResponseEntity<PagamentoResponse> pagar(@PathVariable Long id, @Valid @RequestBody SolicitarPagamentoRequest req,
                                                   @RequestHeader(name = "Idempotency-Key", required = false) String chave) {
        var r = pagamentos.solicitar(id, req.metodo(), chave);
        if (!r.novo()) {
            return ResponseEntity.ok(r.pagamento());
        }
        HttpStatus status = switch (r.pagamento().status()) {
            case FALHA_COMUNICACAO, PENDENTE -> HttpStatus.ACCEPTED;
            default -> HttpStatus.CREATED;
        };
        return ResponseEntity.status(status).body(r.pagamento());
    }

    @GetMapping("/{id}/pagamentos")
    public List<PagamentoResponse> pagamentosDoPedido(@PathVariable Long id) {
        return pagamentos.listarDoPedido(id);
    }
}
