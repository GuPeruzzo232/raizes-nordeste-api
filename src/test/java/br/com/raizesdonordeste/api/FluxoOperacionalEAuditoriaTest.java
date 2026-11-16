package br.com.raizesdonordeste.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Fluxo de preparo, cancelamento, desconto e auditoria")
class FluxoOperacionalEAuditoriaTest extends TesteIntegracaoBase {

    @Test
    @DisplayName("CT35 [+] Fluxo completo: cozinha prepara, atendente entrega")
    void fluxoCompleto() throws Exception {
        long id = criarPedidoPago();
        patch_("/api/pedidos/" + id + "/status", COZINHA_RECIFE, Map.of("status", "EM_PREPARO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_PREPARO"));
        patch_("/api/pedidos/" + id + "/status", COZINHA_RECIFE, Map.of("status", "PRONTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PRONTO"));
        patch_("/api/pedidos/" + id + "/status", ATENDENTE_RECIFE, Map.of("status", "ENTREGUE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    @DisplayName("CT36 [-] Cozinha não marca pedido como ENTREGUE (403)")
    void cozinhaNaoEntrega() throws Exception {
        long id = criarPedidoPago();
        patch_("/api/pedidos/" + id + "/status", COZINHA_RECIFE, Map.of("status", "ENTREGUE")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT37 [-] Transição inválida (não pago → PRONTO) retorna 422")
    void transicaoInvalida() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        patch_("/api/pedidos/" + id + "/status", COZINHA_RECIFE, Map.of("status", "PRONTO"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("Transição inválida")));
    }

    @Test
    @DisplayName("CT38 [+] Fila da cozinha não expõe cliente, valores nem pagamento")
    void filaSemDadosPessoais() throws Exception {
        long id = criarPedidoPago();
        var r = get_("/api/cozinha/fila", COZINHA_RECIFE).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].pedidoId", hasItem((int) id)));
        String corpo = r.andReturn().getResponse().getContentAsString();
        assertThat(corpo).doesNotContain("clienteId", "total", "cpf", "email");
    }

    @Test
    @DisplayName("CT39 [+] Cancelamento pelo atendente devolve estoque e registra quem, quando e por quê")
    void cancelamentoAuditado() throws Exception {
        int antes = estoque(RECIFE, BAIAO);
        long id = criarPedidoMaria(BAIAO, 2);
        assertThat(estoque(RECIFE, BAIAO)).isEqualTo(antes - 2);
        post_("/api/pedidos/" + id + "/cancelamento", ATENDENTE_RECIFE, Map.of("motivo", "Cliente desistiu no balcão"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Cliente desistiu no balcão"));
        assertThat(estoque(RECIFE, BAIAO)).isEqualTo(antes);
        get_("/api/auditoria?acao=CANCELAMENTO_PEDIDO&entidadeId=" + id, GERENTE_RECIFE)
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].usuarioEmail").value(ATENDENTE_RECIFE))
                .andExpect(jsonPath("$.conteudo[0].perfil").value("ATENDENTE"))
                .andExpect(jsonPath("$.conteudo[0].dataHora").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[0].detalhes", containsString("motivo=Cliente desistiu")));
    }

    @Test
    @DisplayName("CT40 [-] Cliente não cancela pedido já pago (422)")
    void clienteNaoCancelaPago() throws Exception {
        long id = criarPedidoPago();
        post_("/api/pedidos/" + id + "/cancelamento", MARIA, Map.of("motivo", "Mudei de ideia")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("CT41 [-] Cliente não aplica desconto (403)")
    void clienteSemDesconto() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        post_("/api/pedidos/" + id + "/desconto", MARIA, Map.of("percentual", 10, "motivo", "quero desconto")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT42 [-] Atendente não ultrapassa 10% de desconto (422)")
    void limiteAtendente() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        post_("/api/pedidos/" + id + "/desconto", ATENDENTE_RECIFE, Map.of("percentual", 15, "motivo", "cortesia"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("excede o limite")));
    }

    @Test
    @DisplayName("CT43 [+] Gerente aplica 20% de desconto: total recalculado e auditoria registrada")
    void descontoGerente() throws Exception {
        long id = criarPedidoMaria(BAIAO, 1); // 32,90
        post_("/api/pedidos/" + id + "/desconto", GERENTE_RECIFE, Map.of("percentual", 20, "motivo", "Atraso na entrega"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desconto").value(6.58))
                .andExpect(jsonPath("$.total").value(26.32));
        get_("/api/auditoria?acao=DESCONTO_PEDIDO&entidadeId=" + id, MATRIZ)
                .andExpect(jsonPath("$.conteudo[0].usuarioEmail").value(GERENTE_RECIFE))
                .andExpect(jsonPath("$.conteudo[0].detalhes", containsString("percentual=20")));
    }

    @Test
    @DisplayName("CT44 [-] Gerente de Salvador não lê auditoria de Recife (403)")
    void auditoriaOutraUnidade() throws Exception {
        get_("/api/auditoria?unidadeId=1", GERENTE_SALVADOR).andExpect(status().isForbidden());
    }
}
