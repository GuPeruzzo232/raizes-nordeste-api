package br.com.raizesdonordeste.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Pedidos multicanal, estoque e escopo de acesso")
class PedidoMulticanalTest extends TesteIntegracaoBase {

    @Test
    @DisplayName("CT15 [+] Cliente cria pedido pelo APP: canal gravado e preço vindo do cardápio da unidade")
    void pedidoApp() throws Exception {
        int estoqueAntes = estoque(RECIFE, TAPIOCA);
        post_("/api/pedidos", MARIA, pedidoApp(RECIFE, TAPIOCA, 2, SUCO_CAJA, 1))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/pedidos/")))
                .andExpect(jsonPath("$.canal").value("APP"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.clienteId").value((int) ID_MARIA))
                .andExpect(jsonPath("$.subtotal").value(38.00))
                .andExpect(jsonPath("$.total").value(38.00))
                .andExpect(jsonPath("$.itens", hasSize(2)));
        assertThat(estoque(RECIFE, TAPIOCA)).isEqualTo(estoqueAntes - 2);
    }

    @Test
    @DisplayName("CT16 [+] Atendente registra venda anônima no BALCAO e no TOTEM")
    void pedidoBalcao() throws Exception {
        post_("/api/pedidos", ATENDENTE_RECIFE, pedido(RECIFE, "BALCAO", BAIAO, 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("BALCAO"))
                .andExpect(jsonPath("$.clienteId").value(nullValue()))
                .andExpect(jsonPath("$.total").value(32.90));
        post_("/api/pedidos", ATENDENTE_RECIFE, pedido(RECIFE, "TOTEM", CUSCUZ, 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("TOTEM"));
    }

    @Test
    @DisplayName("CT17 [+] Pick-up: cliente pede pelo app para retirar na loja (canal PICKUP)")
    void pedidoPickup() throws Exception {
        post_("/api/pedidos", MARIA, pedido(RECIFE, "PICKUP", BAIAO, 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("PICKUP"));
    }

    @Test
    @DisplayName("CT18 [-] Pedido sem canal é rejeitado (400)")
    void semCanal() throws Exception {
        post_("/api/pedidos", MARIA, pedido(RECIFE, null, TAPIOCA, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasItem(startsWith("canal"))));
    }

    @Test
    @DisplayName("CT19 [-] Canal inexistente é rejeitado (400)")
    void canalInvalido() throws Exception {
        post_("/api/pedidos", MARIA, pedido(RECIFE, "FAX", TAPIOCA, 1)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT20 [-] Produto que não está no cardápio da unidade (acarajé em Recife) retorna 422")
    void foraDoCardapio() throws Exception {
        post_("/api/pedidos", MARIA, pedidoApp(RECIFE, ACARAJE, 1))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("não está disponível")));
    }

    @Test
    @DisplayName("CT21 [-] Item junino fora da vigência não pode ser pedido (422)")
    void sazonalForaDeEpoca() throws Exception {
        post_("/api/pedidos", MARIA, pedidoApp(RECIFE, CANJICA, 1)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("CT22 [-] Estoque insuficiente retorna 409 e não baixa nenhum item (rollback)")
    void estoqueInsuficiente() throws Exception {
        int baiaoAntes = estoque(RECIFE, BAIAO);
        int carneAntes = estoque(RECIFE, CARNE_DE_SOL);
        post_("/api/pedidos", MARIA, pedidoApp(RECIFE, BAIAO, 1, CARNE_DE_SOL, carneAntes + 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("Estoque insuficiente")));
        assertThat(estoque(RECIFE, BAIAO)).isEqualTo(baiaoAntes);
        assertThat(estoque(RECIFE, CARNE_DE_SOL)).isEqualTo(carneAntes);
    }

    @Test
    @DisplayName("CT23 [+] Reenvio com a mesma Idempotency-Key devolve o mesmo pedido (sem duplicar)")
    void idempotencia() throws Exception {
        String tk = token(MARIA);
        String corpo = pedidoApp(RECIFE, TAPIOCA, 1);
        var r1 = mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + tk).header("Idempotency-Key", "totem-123-abc")
                .contentType("application/json").content(corpo)).andExpect(status().isCreated());
        int estoqueDepois = estoque(RECIFE, TAPIOCA);
        var r2 = mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + tk).header("Idempotency-Key", "totem-123-abc")
                .contentType("application/json").content(corpo)).andExpect(status().isOk());
        Number id1 = ler(r1, "$.id");
        Number id2 = ler(r2, "$.id");
        assertThat(id2).isEqualTo(id1);
        assertThat(estoque(RECIFE, TAPIOCA)).isEqualTo(estoqueDepois);
    }

    @Test
    @DisplayName("CT24 [-] Cliente não vê pedido de outro cliente (404)")
    void clienteOutroPedido() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        get_("/api/pedidos/" + id, MARIA).andExpect(status().isOk());
        get_("/api/pedidos/" + id, JOAO).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CT25 [-] Atendente de Salvador não acessa pedido de Recife (403)")
    void atendenteOutraUnidade() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        get_("/api/pedidos/" + id, ATENDENTE_SALVADOR).andExpect(status().isForbidden());
        get_("/api/pedidos?unidadeId=1", ATENDENTE_SALVADOR).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT26 [-] Atendente não registra pedido no canal APP nem em outra unidade")
    void atendenteCanalErrado() throws Exception {
        post_("/api/pedidos", ATENDENTE_RECIFE, pedidoApp(RECIFE, TAPIOCA, 1)).andExpect(status().isUnprocessableEntity());
        post_("/api/pedidos", ATENDENTE_RECIFE, pedido(SALVADOR, "BALCAO", TAPIOCA, 1)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT27 [+] Listagem respeita escopo: cliente só vê os próprios pedidos")
    void listagemEscopo() throws Exception {
        criarPedidoMaria(TAPIOCA, 1);
        get_("/api/pedidos?tamanho=100", MARIA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].clienteId", everyItem(is((int) ID_MARIA))));
        get_("/api/pedidos?canal=APP", MATRIZ)
                .andExpect(jsonPath("$.conteudo[*].canal", everyItem(is("APP"))));
    }
}
