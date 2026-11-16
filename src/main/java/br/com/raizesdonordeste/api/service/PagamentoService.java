package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.*;
import br.com.raizesdonordeste.api.exception.ConflitoException;
import br.com.raizesdonordeste.api.exception.CredenciaisInvalidasException;
import br.com.raizesdonordeste.api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.api.exception.RegraNegocioException;
import br.com.raizesdonordeste.api.integration.pagamento.ClientePagamentoResiliente;
import br.com.raizesdonordeste.api.integration.pagamento.GatewayIndisponivelException;
import br.com.raizesdonordeste.api.integration.pagamento.RespostaGateway;
import br.com.raizesdonordeste.api.integration.pagamento.SolicitacaoCobranca;
import br.com.raizesdonordeste.api.repository.EventoPagamentoRecebidoRepository;
import br.com.raizesdonordeste.api.repository.PagamentoRepository;
import br.com.raizesdonordeste.api.web.dto.PagamentoResponse;
import br.com.raizesdonordeste.api.web.dto.WebhookPagamentoRequest;
import br.com.raizesdonordeste.api.web.dto.WebhookResposta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Integração com o serviço externo de pagamento em três fases, para que nenhuma
 * conexão de banco fique presa esperando a rede do provedor:
 * <ol>
 *   <li>transação curta: valida o pedido e grava o pagamento PENDENTE;</li>
 *   <li>fora de transação: chama o provedor (com disjuntor);</li>
 *   <li>transação curta: registra a resposta e atualiza o pedido.</li>
 * </ol>
 * Se o provedor cair, o pagamento fica FALHA_COMUNICACAO e é reprocessado com a MESMA
 * chave de idempotência, então nunca há cobrança dupla. Respostas repetidas (webhook
 * duplicado) são reconhecidas pelo id do evento e ignoradas.
 */
@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentos;
    private final EventoPagamentoRecebidoRepository eventos;
    private final PedidoService pedidoService;
    private final FidelidadeService fidelidade;
    private final AuditoriaService auditoria;
    private final ClientePagamentoResiliente cliente;
    private final TransactionTemplate tx;
    private final String segredoWebhook;
    private final int maxTentativas;

    public PagamentoService(PagamentoRepository pagamentos, EventoPagamentoRecebidoRepository eventos, PedidoService pedidoService,
                            FidelidadeService fidelidade, AuditoriaService auditoria, ClientePagamentoResiliente cliente,
                            PlatformTransactionManager txManager,
                            @Value("${app.pagamento.webhook-segredo}") String segredoWebhook,
                            @Value("${app.pagamento.max-tentativas:5}") int maxTentativas) {
        this.pagamentos = pagamentos;
        this.eventos = eventos;
        this.pedidoService = pedidoService;
        this.fidelidade = fidelidade;
        this.auditoria = auditoria;
        this.cliente = cliente;
        this.tx = new TransactionTemplate(txManager);
        this.segredoWebhook = segredoWebhook;
        this.maxTentativas = maxTentativas;
    }

    public ResultadoPagamento solicitar(Long pedidoId, MetodoPagamento metodo, String idempotencyKey) {
        // Fase 1
        Preparacao prep = tx.execute(s -> preparar(pedidoId, metodo, idempotencyKey));
        if (prep.existente() != null) {
            return new ResultadoPagamento(prep.existente(), false);
        }
        // Fase 2
        RespostaGateway resposta = null;
        String falha = null;
        try {
            resposta = cliente.cobrar(prep.solicitacao());
        } catch (GatewayIndisponivelException e) {
            falha = e.getMessage();
            log.warn("Pagamento {} sem resposta do provedor: {}", prep.solicitacao().pagamentoId(), falha);
        }
        // Fase 3
        final RespostaGateway r = resposta;
        final String f = falha;
        PagamentoResponse resp = tx.execute(s -> registrarResultado(prep.solicitacao().pagamentoId(), r, f));
        return new ResultadoPagamento(resp, true);
    }

    private Preparacao preparar(Long pedidoId, MetodoPagamento metodo, String idempotencyKey) {
        Pedido pedido = pedidoService.carregarComEscopo(pedidoId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existente = pagamentos.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent()) {
                if (!existente.get().getPedido().getId().equals(pedidoId)) {
                    throw new ConflitoException("Idempotency-Key já usada em outro pedido");
                }
                return new Preparacao(null, paraResposta(existente.get()));
            }
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("Pedido com status " + pedido.getStatus() + " não aceita novo pagamento");
        }
        List<Pagamento> anteriores = pagamentos.findByPedidoIdOrderByIdDesc(pedidoId);
        for (Pagamento p : anteriores) {
            if (p.getStatus() == StatusPagamento.PENDENTE || p.getStatus() == StatusPagamento.FALHA_COMUNICACAO) {
                throw new ConflitoException("Já existe pagamento em processamento para este pedido (id " + p.getId()
                        + "). Aguarde a confirmação do provedor.");
            }
        }
        if (pedido.getTotal().signum() <= 0) {
            throw new RegraNegocioException("Pedido sem valor a pagar");
        }
        String chave = (idempotencyKey == null || idempotencyKey.isBlank()) ? "PAG-" + UUID.randomUUID() : idempotencyKey;
        Pagamento pagamento = pagamentos.save(new Pagamento(pedido, pedido.getTotal(), metodo, chave));
        return new Preparacao(new SolicitacaoCobranca(pagamento.getId(), pedidoId, pagamento.getValor(), metodo, chave), null);
    }

    private PagamentoResponse registrarResultado(Long pagamentoId, RespostaGateway resposta, String falha) {
        Pagamento p = pagamentos.findById(pagamentoId).orElseThrow();
        p.registrarTentativa();
        if (p.isFinalizado()) {
            // O webhook chegou antes da resposta síncrona: nada a fazer.
            return paraResposta(p);
        }
        if (resposta == null) {
            p.atualizar(StatusPagamento.FALHA_COMUNICACAO, null, falha);
            return paraResposta(p);
        }
        aplicarStatus(p, resposta.status(), resposta.transacaoId(), resposta.mensagem(), "resposta-sincrona");
        return paraResposta(p);
    }

    /** Reenvia ao provedor os pagamentos que ficaram sem resposta. Chamado pelo agendador. */
    public int reprocessarFalhas() {
        List<Long> ids = tx.execute(s -> pagamentos
                .findTop50ByStatusAndTentativasLessThanOrderByAtualizadoEm(StatusPagamento.FALHA_COMUNICACAO, maxTentativas)
                .stream().map(Pagamento::getId).toList());
        int resolvidos = 0;
        for (Long id : ids) {
            SolicitacaoCobranca s = tx.execute(st -> {
                Pagamento p = pagamentos.findById(id).orElseThrow();
                return new SolicitacaoCobranca(p.getId(), p.getPedido().getId(), p.getValor(), p.getMetodo(), p.getIdempotencyKey());
            });
            try {
                RespostaGateway r = cliente.cobrar(s);
                tx.executeWithoutResult(st -> registrarResultado(id, r, null));
                resolvidos++;
            } catch (GatewayIndisponivelException e) {
                tx.executeWithoutResult(st -> registrarResultado(id, null, e.getMessage()));
                break; // provedor ainda fora: não insiste com os demais nesta rodada
            }
        }
        return resolvidos;
    }

    /** Notificação assíncrona do provedor. Idempotente pelo eventoId. */
    public WebhookResposta receberWebhook(String assinatura, WebhookPagamentoRequest req) {
        if (assinatura == null || !MessageDigest.isEqual(assinatura.getBytes(StandardCharsets.UTF_8), segredoWebhook.getBytes(StandardCharsets.UTF_8))) {
            throw new CredenciaisInvalidasException();
        }
        return tx.execute(s -> {
            if (eventos.existsByEventoId(req.eventoId())) {
                return new WebhookResposta(req.eventoId(), false, "Evento duplicado: já processado anteriormente, ignorado");
            }
            Pagamento p = pagamentos.findById(req.pagamentoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento " + req.pagamentoId() + " não encontrado"));
            if (p.getTransacaoExterna() != null && !p.getTransacaoExterna().equals(req.transacaoExterna())) {
                throw new RegraNegocioException("transacaoExterna não confere com o pagamento");
            }
            eventos.save(new EventoPagamentoRecebido(req.eventoId(), p.getId(), req.status().name()));
            if (p.getStatus() == req.status()) {
                return new WebhookResposta(req.eventoId(), false, "Status já aplicado anteriormente");
            }
            boolean estorno = p.getStatus() == StatusPagamento.APROVADO && req.status() == StatusPagamento.ESTORNADO;
            if (p.isFinalizado() && !estorno) {
                return new WebhookResposta(req.eventoId(), false, "Pagamento já finalizado como " + p.getStatus() + "; transição ignorada");
            }
            aplicarStatus(p, req.status(), req.transacaoExterna(), req.mensagem(), "webhook:" + req.eventoId());
            return new WebhookResposta(req.eventoId(), true, "Status atualizado para " + req.status());
        });
    }

    private void aplicarStatus(Pagamento p, StatusPagamento novo, String transacao, String mensagem, String origem) {
        StatusPagamento anterior = p.getStatus();
        p.atualizar(novo, transacao, mensagem);
        Pedido pedido = p.getPedido();
        if (novo == StatusPagamento.APROVADO) {
            if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
                pedido.alterarStatus(StatusPedido.PAGO);
                fidelidade.creditarPorPedido(pedido);
            } else if (pedido.getStatus() == StatusPedido.CANCELADO) {
                mensagem = "Pagamento aprovado para pedido cancelado: estorno necessário";
                p.atualizar(novo, transacao, mensagem);
            }
        }
        auditoria.registrar(AuditoriaService.PAGAMENTO_ATUALIZADO, "Pagamento", p.getId(), pedido.getUnidade().getId(),
                "pedido=" + pedido.getId() + "; de=" + anterior + "; para=" + novo + "; transacao=" + transacao + "; origem=" + origem);
    }

    public List<PagamentoResponse> listarDoPedido(Long pedidoId) {
        return tx.execute(s -> {
            pedidoService.carregarComEscopo(pedidoId);
            return pagamentos.findByPedidoIdOrderByIdDesc(pedidoId).stream().map(this::paraResposta).toList();
        });
    }

    private PagamentoResponse paraResposta(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getValor(), p.getMetodo(), p.getStatus(),
                p.getPedido().getStatus(), p.getTransacaoExterna(), p.getTentativas(), p.getUltimaMensagem(), p.getAtualizadoEm());
    }

    private record Preparacao(SolicitacaoCobranca solicitacao, PagamentoResponse existente) {
    }

    public record ResultadoPagamento(PagamentoResponse pagamento, boolean novo) {
    }
}
