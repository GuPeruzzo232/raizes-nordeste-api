package br.com.raizesdonordeste.api;

import br.com.raizesdonordeste.api.integration.pagamento.CircuitBreakerSimples;
import br.com.raizesdonordeste.api.integration.pagamento.GatewayPagamentoSimulado.Modo;
import br.com.raizesdonordeste.api.service.PagamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Pagamento externo: aprovação, recusa, queda do provedor e resposta duplicada")
class PagamentoExternoTest extends TesteIntegracaoBase {

    private static final String SEGREDO = "segredo-webhook-dev";

    @Autowired
    PagamentoService pagamentoService;

    @Test
    @DisplayName("CT28 [+] Pagamento aprovado: pedido vira PAGO e cliente com consentimento ganha pontos")
    void aprovado() throws Exception {
        int pontosAntes = ler(get_("/api/clientes/me/fidelidade", MARIA), "$.pontos");
        long id = criarPedidoMaria(TAPIOCA, 2, SUCO_CAJA, 1); // 38,00
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.statusPedido").value("PAGO"))
                .andExpect(jsonPath("$.transacaoExterna", startsWith("SIM-")));
        get_("/api/clientes/me/fidelidade", MARIA).andExpect(jsonPath("$.pontos").value(pontosAntes + 38));
    }

    @Test
    @DisplayName("CT29 [-] Pagamento recusado: pedido continua AGUARDANDO_PAGAMENTO e aceita nova tentativa")
    void recusado() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        gateway.definirModo(Modo.RECUSAR);
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "CARTAO_CREDITO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.statusPedido").value("AGUARDANDO_PAGAMENTO"));
        gateway.definirModo(Modo.APROVAR);
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    @DisplayName("CT30 [-] Provedor fora do ar: 202, pagamento em FALHA_COMUNICACAO e reprocessado depois com a mesma chave")
    void provedorFora() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        gateway.definirModo(Modo.INDISPONIVEL);
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("FALHA_COMUNICACAO"))
                .andExpect(jsonPath("$.statusPedido").value("AGUARDANDO_PAGAMENTO"));
        // nova solicitação enquanto há uma pendente é bloqueada (evita cobrança dupla)
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX"))
                .andExpect(status().isConflict());
        // provedor volta; o agendador reprocessa
        gateway.definirModo(Modo.APROVAR);
        assertThat(pagamentoService.reprocessarFalhas()).isGreaterThanOrEqualTo(1);
        get_("/api/pedidos/" + id + "/pagamentos", MARIA)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("APROVADO"))
                .andExpect(jsonPath("$[0].tentativas").value(2));
        get_("/api/pedidos/" + id, MARIA).andExpect(jsonPath("$.status").value("PAGO"));
    }

    @Test
    @DisplayName("CT31 [-] Webhook repetido (mesmo eventoId) é processado uma única vez")
    void webhookDuplicado() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        gateway.definirModo(Modo.INDISPONIVEL);
        Number pagamentoId = ler(post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX")), "$.id");
        String corpo = json.writeValueAsString(Map.of("eventoId", "evt-" + id, "pagamentoId", pagamentoId,
                "transacaoExterna", "PSP-" + id, "status", "APROVADO"));

        var primeira = mvc.perform(post("/api/pagamentos/webhook").header("X-Webhook-Secret", SEGREDO)
                .contentType("application/json").content(corpo)).andExpect(status().isOk());
        var segunda = mvc.perform(post("/api/pagamentos/webhook").header("X-Webhook-Secret", SEGREDO)
                .contentType("application/json").content(corpo)).andExpect(status().isOk());

        assertThat((Boolean) ler(primeira, "$.processado")).isTrue();
        assertThat((Boolean) ler(segunda, "$.processado")).isFalse();
        assertThat((String) ler(segunda, "$.observacao")).contains("duplicado");
        get_("/api/pedidos/" + id, MARIA).andExpect(jsonPath("$.status").value("PAGO"));
        get_("/api/auditoria?acao=PAGAMENTO_ATUALIZADO&entidade=Pagamento&entidadeId=" + pagamentoId, MATRIZ)
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("CT32 [-] Webhook sem o segredo compartilhado é rejeitado (401)")
    void webhookSemSegredo() throws Exception {
        String corpo = json.writeValueAsString(Map.of("eventoId", "evt-x", "pagamentoId", 1, "transacaoExterna", "x", "status", "APROVADO"));
        mvc.perform(post("/api/pagamentos/webhook").header("X-Webhook-Secret", "errado")
                .contentType("application/json").content(corpo)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CT33 [-] Pagar pedido já pago retorna 409")
    void pagarDuasVezes() throws Exception {
        long id = criarPedidoPago();
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CT34 [-] Disjuntor abre após 3 falhas seguidas e passa a falhar sem chamar o provedor")
    void disjuntor() throws Exception {
        gateway.definirModo(Modo.INDISPONIVEL);
        for (int i = 0; i < 3; i++) {
            long id = criarPedidoMaria(TAPIOCA, 1);
            post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX")).andExpect(status().isAccepted());
        }
        assertThat(clientePagamento.getDisjuntor().getEstado()).isEqualTo(CircuitBreakerSimples.Estado.ABERTO);
        int chamadas = gateway.totalChamadas();
        long id = criarPedidoMaria(TAPIOCA, 1);
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem", containsString("Circuito aberto")));
        assertThat(gateway.totalChamadas()).isEqualTo(chamadas);
    }
}
