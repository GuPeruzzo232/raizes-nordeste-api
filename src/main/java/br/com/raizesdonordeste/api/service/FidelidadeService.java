package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.*;
import br.com.raizesdonordeste.api.repository.ConsentimentoRepository;
import br.com.raizesdonordeste.api.repository.ContaFidelidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Programa de fidelidade: 1 ponto por real pago, somente com consentimento vigente. */
@Service
public class FidelidadeService {

    private final ContaFidelidadeRepository contas;
    private final ConsentimentoRepository consentimentos;

    public FidelidadeService(ContaFidelidadeRepository contas, ConsentimentoRepository consentimentos) {
        this.contas = contas;
        this.consentimentos = consentimentos;
    }

    @Transactional(readOnly = true)
    public boolean participa(Long usuarioId) {
        return consentimentos.findFirstByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(usuarioId, FinalidadeConsentimento.PROGRAMA_FIDELIDADE)
                .map(Consentimento::isConcedido)
                .orElse(false);
    }

    @Transactional
    public void creditarPorPedido(Pedido pedido) {
        Usuario cliente = pedido.getCliente();
        if (cliente == null || !participa(cliente.getId())) {
            return;
        }
        ContaFidelidade conta = contas.findByUsuarioId(cliente.getId()).orElseGet(() -> contas.save(new ContaFidelidade(cliente)));
        conta.creditar(pedido.getTotal().intValue());
    }

    @Transactional(readOnly = true)
    public int pontos(Long usuarioId) {
        return contas.findByUsuarioId(usuarioId).map(ContaFidelidade::getPontos).orElse(0);
    }
}
