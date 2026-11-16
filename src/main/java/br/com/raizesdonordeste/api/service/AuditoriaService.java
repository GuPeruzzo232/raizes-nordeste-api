package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.RegistroAuditoria;
import br.com.raizesdonordeste.api.exception.AcessoNegadoException;
import br.com.raizesdonordeste.api.repository.RegistroAuditoriaRepository;
import br.com.raizesdonordeste.api.security.UsuarioAtual;
import br.com.raizesdonordeste.api.security.UsuarioAutenticado;
import br.com.raizesdonordeste.api.web.dto.AuditoriaResponse;
import br.com.raizesdonordeste.api.web.dto.PaginaResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Grava a trilha de auditoria na mesma transação da operação: se a operação
 * falhar, o registro também é desfeito, e nunca há operação sem registro.
 */
@Service
public class AuditoriaService {

    public static final String CANCELAMENTO_PEDIDO = "CANCELAMENTO_PEDIDO";
    public static final String DESCONTO_PEDIDO = "DESCONTO_PEDIDO";
    public static final String AJUSTE_ESTOQUE = "AJUSTE_ESTOQUE";
    public static final String ALTERACAO_CARDAPIO = "ALTERACAO_CARDAPIO";
    public static final String ALTERACAO_STATUS_PEDIDO = "ALTERACAO_STATUS_PEDIDO";
    public static final String PAGAMENTO_ATUALIZADO = "PAGAMENTO_ATUALIZADO";
    public static final String ACESSO_DADOS_PESSOAIS = "ACESSO_DADOS_PESSOAIS";
    public static final String CONSENTIMENTO_ALTERADO = "CONSENTIMENTO_ALTERADO";
    public static final String ANONIMIZACAO = "ANONIMIZACAO";

    private final RegistroAuditoriaRepository repository;
    private final UsuarioAtual usuarioAtual;

    public AuditoriaService(RegistroAuditoriaRepository repository, UsuarioAtual usuarioAtual) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public void registrar(String acao, String entidade, Long entidadeId, Long unidadeId, String detalhes) {
        var usuario = usuarioAtual.talvez();
        repository.save(new RegistroAuditoria(
                usuario.map(UsuarioAutenticado::id).orElse(null),
                usuario.map(UsuarioAutenticado::email).orElse("sistema"),
                usuario.map(u -> u.perfil().name()).orElse("SISTEMA"),
                unidadeId,
                acao,
                entidade,
                entidadeId,
                detalhes,
                ipDaRequisicao()));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> pesquisar(Long unidadeId, String acao, String entidade, Long entidadeId, int pagina, int tamanho) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Long filtroUnidade = unidadeId;
        if (!u.is(br.com.raizesdonordeste.api.domain.Perfil.MATRIZ)) {
            // Gerente só audita a própria loja
            if (unidadeId != null && !unidadeId.equals(u.unidadeId())) {
                throw new AcessoNegadoException("Gerente só consulta a auditoria da própria unidade");
            }
            filtroUnidade = u.unidadeId();
        }
        var page = repository.pesquisar(filtroUnidade, acao, entidade, entidadeId,
                        PageRequest.of(pagina, Math.min(tamanho, 100), Sort.by(Sort.Direction.DESC, "dataHora", "id")))
                .map(r -> new AuditoriaResponse(r.getId(), r.getDataHora(), r.getUsuarioId(), r.getUsuarioEmail(), r.getPerfil(),
                        r.getUnidadeId(), r.getAcao(), r.getEntidade(), r.getEntidadeId(), r.getDetalhes(), r.getIp()));
        return PaginaResponse.de(page);
    }

    private String ipDaRequisicao() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String encaminhado = req.getHeader("X-Forwarded-For");
            return encaminhado != null ? encaminhado.split(",")[0].trim() : req.getRemoteAddr();
        }
        return null;
    }
}
