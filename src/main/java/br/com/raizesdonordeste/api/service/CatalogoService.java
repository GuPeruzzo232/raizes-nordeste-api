package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.*;
import br.com.raizesdonordeste.api.exception.AcessoNegadoException;
import br.com.raizesdonordeste.api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.api.exception.RegraNegocioException;
import br.com.raizesdonordeste.api.repository.*;
import br.com.raizesdonordeste.api.security.UsuarioAtual;
import br.com.raizesdonordeste.api.security.UsuarioAutenticado;
import br.com.raizesdonordeste.api.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Unidades, produtos, cardápio por unidade (com sazonalidade) e estoque por unidade. */
@Service
public class CatalogoService {

    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final ItemCardapioRepository cardapio;
    private final EstoqueRepository estoques;
    private final AuditoriaService auditoria;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public CatalogoService(UnidadeRepository unidades, ProdutoRepository produtos, ItemCardapioRepository cardapio,
                           EstoqueRepository estoques, AuditoriaService auditoria, UsuarioAtual usuarioAtual, Clock clock) {
        this.unidades = unidades;
        this.produtos = produtos;
        this.cardapio = cardapio;
        this.estoques = estoques;
        this.auditoria = auditoria;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UnidadeResponse> listarUnidades() {
        return unidades.findByAtivaTrueOrderByNome().stream()
                .map(u -> new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.getUf()))
                .toList();
    }

    /**
     * Cardápio vigente de uma unidade na data informada (padrão: hoje).
     * Itens sazonais fora da janela de vigência não aparecem.
     */
    @Transactional(readOnly = true)
    public List<ItemCardapioResponse> cardapio(Long unidadeId, LocalDate data) {
        buscarUnidade(unidadeId);
        LocalDate dia = data != null ? data : LocalDate.now(clock);
        Map<Long, Integer> saldo = estoques.findByUnidade(unidadeId).stream()
                .collect(Collectors.toMap(e -> e.getProduto().getId(), Estoque::getQuantidade));
        return cardapio.findByUnidadeComProduto(unidadeId).stream()
                .filter(i -> i.vigenteEm(dia))
                .map(i -> new ItemCardapioResponse(i.getProduto().getId(), i.getProduto().getNome(), i.getProduto().getDescricao(),
                        i.getProduto().getCategoria(), i.getPreco(), i.getProduto().isSazonal(), i.getFimVigencia(),
                        saldo.getOrDefault(i.getProduto().getId(), 0) > 0))
                .toList();
    }

    @Transactional
    public ProdutoResponse criarProduto(ProdutoRequest req) {
        Produto p = produtos.save(new Produto(req.nome(), req.descricao(), req.categoria(), req.precoBase(), req.sazonal()));
        return paraResposta(p);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProdutos() {
        return produtos.findAll().stream().map(this::paraResposta).toList();
    }

    /** Inclui ou altera um item no cardápio da unidade (preço local, disponibilidade, vigência). */
    @Transactional
    public ItemCardapioResponse salvarItemCardapio(Long unidadeId, ItemCardapioRequest req) {
        exigirAcessoUnidade(unidadeId);
        Unidade unidade = buscarUnidade(unidadeId);
        Produto produto = produtos.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + req.produtoId() + " não encontrado"));
        if (req.inicioVigencia() != null && req.fimVigencia() != null && req.fimVigencia().isBefore(req.inicioVigencia())) {
            throw new RegraNegocioException("fimVigencia não pode ser anterior a inicioVigencia");
        }
        if (produto.isSazonal() && req.fimVigencia() == null) {
            throw new RegraNegocioException("Produto sazonal exige data de fim de vigência no cardápio");
        }
        ItemCardapio item = cardapio.findByUnidadeAndProduto(unidadeId, produto.getId())
                .orElseGet(() -> new ItemCardapio(unidade, produto, req.preco(), req.inicioVigencia(), req.fimVigencia()));
        item.setPreco(req.preco());
        item.setInicioVigencia(req.inicioVigencia());
        item.setFimVigencia(req.fimVigencia());
        item.setDisponivel(req.disponivel() == null || req.disponivel());
        cardapio.save(item);
        if (estoques.findByUnidadeAndProduto(unidadeId, produto.getId()).isEmpty()) {
            estoques.save(new Estoque(unidade, produto, 0));
        }
        auditoria.registrar(AuditoriaService.ALTERACAO_CARDAPIO, "ItemCardapio", item.getId(), unidadeId,
                "produto=" + produto.getId() + "; preco=" + req.preco() + "; disponivel=" + item.isDisponivel()
                        + "; vigencia=" + req.inicioVigencia() + ".." + req.fimVigencia());
        return new ItemCardapioResponse(produto.getId(), produto.getNome(), produto.getDescricao(), produto.getCategoria(),
                item.getPreco(), produto.isSazonal(), item.getFimVigencia(), false);
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponse> estoque(Long unidadeId) {
        exigirAcessoUnidade(unidadeId);
        buscarUnidade(unidadeId);
        return estoques.findByUnidade(unidadeId).stream().map(this::paraResposta).toList();
    }

    /** Ajuste manual (inventário/perda/recebimento): registra valor anterior, novo e motivo. */
    @Transactional
    public EstoqueResponse ajustarEstoque(Long unidadeId, Long produtoId, AjusteEstoqueRequest req) {
        exigirAcessoUnidade(unidadeId);
        Estoque e = estoques.findByUnidadeAndProduto(unidadeId, produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + produtoId + " não tem estoque nesta unidade"));
        int anterior = e.getQuantidade();
        e.definirQuantidade(req.quantidade());
        auditoria.registrar(AuditoriaService.AJUSTE_ESTOQUE, "Estoque", e.getId(), unidadeId,
                "produto=" + produtoId + "; de=" + anterior + "; para=" + req.quantidade() + "; motivo=" + req.motivo());
        return paraResposta(e);
    }

    Unidade buscarUnidade(Long id) {
        return unidades.findById(id)
                .filter(Unidade::isAtiva)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade " + id + " não encontrada"));
    }

    private void exigirAcessoUnidade(Long unidadeId) {
        UsuarioAutenticado u = usuarioAtual.obter();
        if (!u.podeAcessarUnidade(unidadeId)) {
            throw new AcessoNegadoException("Você só pode gerenciar a sua própria unidade");
        }
    }

    private ProdutoResponse paraResposta(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getPrecoBase(), p.isSazonal(), p.isAtivo());
    }

    private EstoqueResponse paraResposta(Estoque e) {
        return new EstoqueResponse(e.getUnidade().getId(), e.getProduto().getId(), e.getProduto().getNome(), e.getQuantidade(), e.getAtualizadoEm());
    }

}
