package br.com.raizesdonordeste.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Cardápio e estoque por unidade, itens sazonais")
class CardapioEEstoqueTest extends TesteIntegracaoBase {

    @Test
    @DisplayName("CT09 [+] Cardápio varia por unidade: acarajé só em Salvador, preço local diferente")
    void cardapioPorUnidade() throws Exception {
        get_("/api/unidades/1/cardapio", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", not(hasItem("Acarajé"))))
                .andExpect(jsonPath("$[?(@.nome=='Baião de dois')].preco", contains(32.90)));
        get_("/api/unidades/2/cardapio", null)
                .andExpect(jsonPath("$[*].nome", hasItem("Acarajé")))
                .andExpect(jsonPath("$[?(@.nome=='Baião de dois')].preco", contains(34.90)));
    }

    @Test
    @DisplayName("CT10 [+] Item junino aparece em junho e some em novembro")
    void sazonal() throws Exception {
        get_("/api/unidades/1/cardapio?data=2026-06-20", null)
                .andExpect(jsonPath("$[*].nome", hasItem("Canjica junina")))
                .andExpect(jsonPath("$[?(@.nome=='Canjica junina')].sazonal", contains(true)));
        get_("/api/unidades/1/cardapio", null) // hoje = 16/11/2026
                .andExpect(jsonPath("$[*].nome", not(hasItem("Canjica junina"))));
    }

    @Test
    @DisplayName("CT11 [-] Cardápio de unidade inexistente retorna 404")
    void unidadeInexistente() throws Exception {
        get_("/api/unidades/999/cardapio", null).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CT12 [-] Gerente de Salvador não ajusta estoque de Recife (403)")
    void ajusteOutraUnidade() throws Exception {
        put_("/api/unidades/1/estoque/" + BAIAO, GERENTE_SALVADOR, Map.of("quantidade", 10, "motivo", "teste indevido"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT13 [+] Ajuste de estoque pelo gerente fica registrado na auditoria")
    void ajusteAuditado() throws Exception {
        int antes = estoque(RECIFE, CUSCUZ);
        put_("/api/unidades/1/estoque/" + CUSCUZ, GERENTE_RECIFE, Map.of("quantidade", antes + 20, "motivo", "Recebimento de fornecedor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(antes + 20));
        get_("/api/auditoria?acao=AJUSTE_ESTOQUE", GERENTE_RECIFE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].usuarioEmail").value(GERENTE_RECIFE))
                .andExpect(jsonPath("$.conteudo[0].detalhes", containsString("de=" + antes + "; para=" + (antes + 20))))
                .andExpect(jsonPath("$.conteudo[0].detalhes", containsString("Recebimento de fornecedor")));
    }

    @Test
    @DisplayName("CT14 [-] Gerente não cadastra produto corporativo (somente matriz)")
    void produtoSoMatriz() throws Exception {
        var corpo = Map.of("nome", "Bolo de rolo", "categoria", "Sobremesas", "precoBase", 12.0, "sazonal", false);
        post_("/api/produtos", GERENTE_RECIFE, corpo).andExpect(status().isForbidden());
        post_("/api/produtos", MATRIZ, corpo).andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
    }
}
