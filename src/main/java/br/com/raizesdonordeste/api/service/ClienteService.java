package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.*;
import br.com.raizesdonordeste.api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.api.repository.ConsentimentoRepository;
import br.com.raizesdonordeste.api.repository.UsuarioRepository;
import br.com.raizesdonordeste.api.security.UsuarioAtual;
import br.com.raizesdonordeste.api.security.UsuarioAutenticado;
import br.com.raizesdonordeste.api.web.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Dados pessoais do cliente sob a LGPD: acesso registrado, consentimento versionado e anonimização. */
@Service
public class ClienteService {

    private final UsuarioRepository usuarios;
    private final ConsentimentoRepository consentimentos;
    private final FidelidadeService fidelidade;
    private final AuditoriaService auditoria;
    private final UsuarioAtual usuarioAtual;
    private final PasswordEncoder encoder;
    private final String versaoTermo;

    public ClienteService(UsuarioRepository usuarios, ConsentimentoRepository consentimentos, FidelidadeService fidelidade,
                          AuditoriaService auditoria, UsuarioAtual usuarioAtual, PasswordEncoder encoder,
                          @Value("${app.lgpd.versao-termo:2026.1}") String versaoTermo) {
        this.usuarios = usuarios;
        this.consentimentos = consentimentos;
        this.fidelidade = fidelidade;
        this.auditoria = auditoria;
        this.usuarioAtual = usuarioAtual;
        this.encoder = encoder;
        this.versaoTermo = versaoTermo;
    }

    /** Titular consultando os próprios dados (direito de acesso, art. 18, II). */
    @Transactional(readOnly = true)
    public ClienteResponse meusDados() {
        UsuarioAutenticado u = usuarioAtual.obter();
        return paraResposta(buscarCliente(u.id()), false);
    }

    /**
     * Funcionário consultando dados de um cliente. Gera registro ACESSO_DADOS_PESSOAIS
     * com quem acessou e quando. Gerente vê CPF mascarado; matriz vê completo.
     */
    @Transactional
    public ClienteResponse consultarPorFuncionario(Long clienteId, String finalidade) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Usuario c = buscarCliente(clienteId);
        auditoria.registrar(AuditoriaService.ACESSO_DADOS_PESSOAIS, "Cliente", clienteId, u.unidadeId(),
                "finalidade=" + (finalidade == null ? "nao informada" : finalidade));
        return paraResposta(c, !u.is(Perfil.MATRIZ));
    }

    @Transactional
    public List<ConsentimentoResponse> registrarConsentimento(ConsentimentoRequest req) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Usuario c = buscarCliente(u.id());
        consentimentos.save(new Consentimento(c, req.finalidade(), req.concedido(), versaoTermo, req.canalOrigem()));
        auditoria.registrar(AuditoriaService.CONSENTIMENTO_ALTERADO, "Cliente", c.getId(), null,
                "finalidade=" + req.finalidade() + "; concedido=" + req.concedido() + "; versaoTermo=" + versaoTermo);
        return historico(c.getId());
    }

    @Transactional(readOnly = true)
    public FidelidadeResponse minhaFidelidade() {
        UsuarioAutenticado u = usuarioAtual.obter();
        return new FidelidadeResponse(u.id(), fidelidade.participa(u.id()), fidelidade.pontos(u.id()));
    }

    /**
     * Direito à eliminação (art. 18, VI): os dados pessoais são substituídos, os consentimentos
     * revogados e o login desativado. Pedidos permanecem (obrigação fiscal), sem vínculo identificável.
     */
    @Transactional
    public void anonimizarMinhaConta() {
        UsuarioAutenticado u = usuarioAtual.obter();
        Usuario c = buscarCliente(u.id());
        for (FinalidadeConsentimento f : FinalidadeConsentimento.values()) {
            consentimentos.save(new Consentimento(c, f, false, versaoTermo, "ANONIMIZACAO"));
        }
        c.anonimizar(encoder.encode(UUID.randomUUID().toString()));
        auditoria.registrar(AuditoriaService.ANONIMIZACAO, "Cliente", c.getId(), null, "solicitado pelo titular");
    }

    private Usuario buscarCliente(Long id) {
        return usuarios.findById(id)
                .filter(x -> x.getPerfil() == Perfil.CLIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + id + " não encontrado"));
    }

    private List<ConsentimentoResponse> historico(Long clienteId) {
        return consentimentos.findByUsuarioIdOrderByRegistradoEmDescIdDesc(clienteId).stream()
                .map(x -> new ConsentimentoResponse(x.getFinalidade(), x.isConcedido(), x.getVersaoTermo(), x.getCanalOrigem(), x.getRegistradoEm()))
                .toList();
    }

    private ClienteResponse paraResposta(Usuario c, boolean mascarar) {
        String cpf = c.getCpf();
        if (mascarar && cpf != null && cpf.length() == 11) {
            cpf = "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
        }
        return new ClienteResponse(c.getId(), c.getNome(), c.getEmail(), cpf, c.getTelefone(), c.isAnonimizado(), c.getCriadoEm(),
                fidelidade.pontos(c.getId()), historico(c.getId()));
    }
}
