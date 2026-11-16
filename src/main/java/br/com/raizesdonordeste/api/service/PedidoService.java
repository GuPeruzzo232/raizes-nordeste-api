package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.*;
import br.com.raizesdonordeste.api.exception.*;
import br.com.raizesdonordeste.api.repository.*;
import br.com.raizesdonordeste.api.security.UsuarioAtual;
import br.com.raizesdonordeste.api.security.UsuarioAutenticado;
import br.com.raizesdonordeste.api.web.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PedidoService {

    /** Canais em que o pedido é registrado por um funcionário da loja. */
    private static final Set<CanalPedido> CANAIS_DE_LOJA = EnumSet.of(CanalPedido.BALCAO, CanalPedido.TOTEM);

    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final ItemCardapioRepository cardapio;
    private final EstoqueRepository estoques;
    private final PagamentoRepository pagamentos;
    private final CatalogoService catalogo;
    private final AuditoriaService auditoria;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;
    private final BigDecimal limiteDescontoGerente;

    public PedidoService(PedidoRepository pedidos, UsuarioRepository usuarios, ItemCardapioRepository cardapio,
                         EstoqueRepository estoques, PagamentoRepository pagamentos, CatalogoService catalogo,
                         AuditoriaService auditoria, UsuarioAtual usuarioAtual, Clock clock,
                         @Value("${app.pedido.limite-desconto-gerente-percentual:30}") BigDecimal limiteDescontoGerente) {
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.cardapio = cardapio;
        this.estoques = estoques;
        this.pagamentos = pagamentos;
        this.catalogo = catalogo;
        this.auditoria = auditoria;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
        this.limiteDescontoGerente = limiteDescontoGerente;
    }

    /**
     * Cria o pedido a partir de qualquer canal. O preço sempre vem do cardápio da unidade
     * (nunca do cliente) e o estoque é baixado de forma atômica. Se a mesma Idempotency-Key
     * for reenviada (ex.: o totem perdeu a resposta e tentou de novo), devolve o pedido já criado.
     */
    @Transactional
    public ResultadoCriacao criar(CriarPedidoRequest req, String idempotencyKey) {
        UsuarioAutenticado u = usuarioAtual.obter();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existente = pedidos.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent()) {
                Pedido p = existente.get();
                if (!p.getCriadoPor().getId().equals(u.id())) {
                    throw new ConflitoException("Idempotency-Key já utilizada por outro usuário");
                }
                return new ResultadoCriacao(paraResposta(p), false);
            }
        }

        Unidade unidade = catalogo.buscarUnidade(req.unidadeId());
        Usuario criadoPor = usuarios.getReferenceById(u.id());
        Usuario cliente = resolverCliente(u, req);
        validarCanalPorPerfil(u, req);

        Pedido pedido = new Pedido(unidade, cliente, criadoPor, req.canal(), req.observacao(), blankParaNull(idempotencyKey));
        LocalDate hoje = LocalDate.now(clock);
        Set<Long> produtosVistos = new HashSet<>();
        for (CriarPedidoRequest.Item item : req.itens()) {
            if (!produtosVistos.add(item.produtoId())) {
                throw new RegraNegocioException("Produto " + item.produtoId() + " repetido no pedido; informe a quantidade total em um único item");
            }
            ItemCardapio ic = cardapio.findByUnidadeAndProduto(unidade.getId(), item.produtoId())
                    .filter(i -> i.vigenteEm(hoje))
                    .orElseThrow(() -> new RegraNegocioException("Produto " + item.produtoId() + " não está disponível no cardápio desta unidade"));
            int baixou = estoques.baixar(unidade.getId(), item.produtoId(), item.quantidade(), Instant.now());
            if (baixou == 0) {
                // Lançar exceção desfaz a transação inteira, inclusive baixas de itens anteriores.
                throw new ConflitoException("Estoque insuficiente para o produto " + ic.getProduto().getNome());
            }
            pedido.adicionarItem(ic.getProduto(), item.quantidade(), ic.getPreco());
        }
        pedidos.save(pedido);
        return new ResultadoCriacao(paraResposta(pedido), true);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long id) {
        return paraResposta(carregarComEscopo(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PedidoResponse> listar(Long unidadeId, StatusPedido status, CanalPedido canal, int pagina, int tamanho) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Long filtroUnidade = unidadeId;
        Long filtroCliente = null;
        if (u.is(Perfil.CLIENTE)) {
            filtroCliente = u.id();
        } else if (!u.is(Perfil.MATRIZ)) {
            if (unidadeId != null && !unidadeId.equals(u.unidadeId())) {
                throw new AcessoNegadoException("Você só pode consultar pedidos da sua unidade");
            }
            filtroUnidade = u.unidadeId();
        }
        var page = pedidos.pesquisar(filtroUnidade, filtroCliente, status, canal,
                        PageRequest.of(pagina, Math.min(tamanho, 100), Sort.by(Sort.Direction.DESC, "criadoEm", "id")))
                .map(this::paraResposta);
        return PaginaResponse.de(page);
    }

    /** Fila da cozinha: pedidos pagos ou em preparo da unidade, sem dados pessoais nem valores. */
    @Transactional(readOnly = true)
    public List<FilaCozinhaResponse> filaCozinha(Long unidadeId) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Long unidade = unidadeId != null ? unidadeId : u.unidadeId();
        if (unidade == null || !u.podeAcessarUnidade(unidade)) {
            throw new AcessoNegadoException("Informe uma unidade à qual você tenha acesso");
        }
        return pedidos.filaDaUnidade(unidade, EnumSet.of(StatusPedido.PAGO, StatusPedido.EM_PREPARO, StatusPedido.PRONTO)).stream()
                .map(p -> new FilaCozinhaResponse(p.getId(), p.getCanal(), p.getStatus(), p.getObservacao(), p.getCriadoEm(),
                        p.getItens().stream().map(i -> new FilaCozinhaResponse.Item(i.getProduto().getNome(), i.getQuantidade())).toList()))
                .toList();
    }

    /**
     * Avanço operacional do pedido. Cozinha: PAGO→EM_PREPARO→PRONTO. Atendente: PRONTO→ENTREGUE.
     * Gerente pode qualquer transição válida. Pagamento e cancelamento têm fluxos próprios.
     */
    @Transactional
    public PedidoResponse alterarStatus(Long id, StatusPedido novo) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Pedido p = carregarComEscopo(id);
        if (novo == StatusPedido.PAGO || novo == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Use os endpoints de pagamento ou de cancelamento para este status");
        }
        boolean permitido = switch (novo) {
            case EM_PREPARO, PRONTO -> u.is(Perfil.COZINHA, Perfil.GERENTE);
            case ENTREGUE -> u.is(Perfil.ATENDENTE, Perfil.GERENTE);
            default -> false;
        };
        if (!permitido) {
            throw new AcessoNegadoException("Seu perfil não pode mover o pedido para " + novo);
        }
        if (!p.getStatus().podeIrPara(novo)) {
            throw new RegraNegocioException("Transição inválida: " + p.getStatus() + " → " + novo);
        }
        StatusPedido anterior = p.getStatus();
        p.alterarStatus(novo);
        auditoria.registrar(AuditoriaService.ALTERACAO_STATUS_PEDIDO, "Pedido", p.getId(), p.getUnidade().getId(),
                "de=" + anterior + "; para=" + novo);
        return paraResposta(p);
    }

    /**
     * Cancela o pedido, devolve os itens ao estoque e registra quem, quando e por quê.
     * Cliente só cancela o próprio pedido enquanto ainda não foi pago.
     */
    @Transactional
    public PedidoResponse cancelar(Long id, String motivo) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Pedido p = carregarComEscopo(id);
        if (u.is(Perfil.CLIENTE) && p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("Pedido já pago: solicite o cancelamento ao atendente da loja");
        }
        if (u.is(Perfil.COZINHA)) {
            throw new AcessoNegadoException("Cozinha não cancela pedidos");
        }
        if (!p.getStatus().podeIrPara(StatusPedido.CANCELADO)) {
            throw new RegraNegocioException("Pedido com status " + p.getStatus() + " não pode ser cancelado");
        }
        StatusPedido anterior = p.getStatus();
        for (ItemPedido item : p.getItens()) {
            estoques.repor(p.getUnidade().getId(), item.getProduto().getId(), item.getQuantidade(), Instant.now());
        }
        boolean pagoAntes = pagamentos.existsByPedidoIdAndStatus(p.getId(), StatusPagamento.APROVADO);
        p.cancelar(motivo);
        auditoria.registrar(AuditoriaService.CANCELAMENTO_PEDIDO, "Pedido", p.getId(), p.getUnidade().getId(),
                "statusAnterior=" + anterior + "; total=" + p.getTotal() + "; estornoNecessario=" + pagoAntes + "; motivo=" + motivo);
        return paraResposta(p);
    }

    /** Desconto percentual. Atendente até 10%, gerente até o limite configurado (padrão 30%). Sempre auditado. */
    @Transactional
    public PedidoResponse aplicarDesconto(Long id, DescontoRequest req) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Pedido p = carregarComEscopo(id);
        if (p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("Desconto só pode ser aplicado antes do pagamento");
        }
        BigDecimal limite = u.is(Perfil.ATENDENTE) ? BigDecimal.TEN : limiteDescontoGerente;
        if (u.is(Perfil.MATRIZ)) {
            limite = BigDecimal.valueOf(100);
        }
        if (req.percentual().compareTo(limite) > 0) {
            throw new RegraNegocioException("Desconto de " + req.percentual() + "% excede o limite de " + limite + "% do seu perfil");
        }
        BigDecimal anterior = p.getDesconto();
        BigDecimal valor = p.getSubtotal().multiply(req.percentual()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        p.aplicarDesconto(valor);
        auditoria.registrar(AuditoriaService.DESCONTO_PEDIDO, "Pedido", p.getId(), p.getUnidade().getId(),
                "percentual=" + req.percentual() + "; valor=" + valor + "; descontoAnterior=" + anterior
                        + "; subtotal=" + p.getSubtotal() + "; motivo=" + req.motivo());
        return paraResposta(p);
    }

    /**
     * Carrega o pedido e aplica o escopo do perfil: cliente só vê os próprios;
     * funcionários de loja só os da sua unidade; matriz vê todos.
     */
    Pedido carregarComEscopo(Long id) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Pedido p = pedidos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + id + " não encontrado"));
        if (u.is(Perfil.CLIENTE)) {
            boolean dono = p.getCliente() != null && p.getCliente().getId().equals(u.id());
            if (!dono) {
                // 404 em vez de 403 para não revelar a existência de pedidos de outras pessoas
                throw new RecursoNaoEncontradoException("Pedido " + id + " não encontrado");
            }
        } else if (!u.podeAcessarUnidade(p.getUnidade().getId())) {
            throw new AcessoNegadoException("Pedido pertence a outra unidade");
        }
        return p;
    }

    private Usuario resolverCliente(UsuarioAutenticado u, CriarPedidoRequest req) {
        if (u.is(Perfil.CLIENTE)) {
            if (req.clienteId() != null && !req.clienteId().equals(u.id())) {
                throw new AcessoNegadoException("Cliente não pode criar pedido em nome de outra pessoa");
            }
            return usuarios.getReferenceById(u.id());
        }
        if (req.clienteId() == null) {
            return null; // venda anônima no balcão/totem
        }
        Usuario c = usuarios.findById(req.clienteId())
                .filter(x -> x.getPerfil() == Perfil.CLIENTE && x.isAtivo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + req.clienteId() + " não encontrado"));
        return c;
    }

    private void validarCanalPorPerfil(UsuarioAutenticado u, CriarPedidoRequest req) {
        if (u.is(Perfil.COZINHA)) {
            throw new AcessoNegadoException("Cozinha não registra pedidos");
        }
        if (u.is(Perfil.CLIENTE) && req.canal() == CanalPedido.BALCAO) {
            throw new RegraNegocioException("Canal BALCAO é exclusivo do atendente");
        }
        if (u.is(Perfil.ATENDENTE, Perfil.GERENTE)) {
            if (!u.podeAcessarUnidade(req.unidadeId())) {
                throw new AcessoNegadoException("Você só pode registrar pedidos na sua unidade");
            }
            if (!CANAIS_DE_LOJA.contains(req.canal())) {
                throw new RegraNegocioException("Funcionário da loja só registra pedidos nos canais BALCAO ou TOTEM");
            }
        }
    }

    PedidoResponse paraResposta(Pedido p) {
        return new PedidoResponse(p.getId(), p.getUnidade().getId(), p.getUnidade().getNome(),
                p.getCliente() != null ? p.getCliente().getId() : null, p.getCanal(), p.getStatus(),
                p.getSubtotal(), p.getDesconto(), p.getTotal(), p.getObservacao(), p.getMotivoCancelamento(),
                p.getCriadoEm(), p.getAtualizadoEm(),
                p.getItens().stream().map(i -> new PedidoResponse.Item(i.getProduto().getId(), i.getProduto().getNome(),
                        i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal())).toList());
    }

    private static String blankParaNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    public record ResultadoCriacao(PedidoResponse pedido, boolean criado) {
    }
}
