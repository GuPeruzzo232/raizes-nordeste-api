package br.com.raizesdonordeste.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LGPD (consentimento, acesso registrado, anonimização) e relatórios")
class LgpdERelatorioTest extends TesteIntegracaoBase {

    @Test
    @DisplayName("CT45 [+] Gerente consulta cliente: CPF mascarado e acesso registrado na auditoria")
    void acessoRegistrado() throws Exception {
        get_("/api/clientes/" + ID_MARIA + "?finalidade=atendimento-reclamacao", GERENTE_RECIFE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("***.456.789-**"));
        get_("/api/auditoria?acao=ACESSO_DADOS_PESSOAIS&entidadeId=" + ID_MARIA, MATRIZ)
                .andExpect(jsonPath("$.conteudo[0].usuarioEmail").value(GERENTE_RECIFE))
                .andExpect(jsonPath("$.conteudo[0].detalhes", containsString("atendimento-reclamacao")));
        get_("/api/clientes/" + ID_MARIA, MATRIZ).andExpect(jsonPath("$.cpf").value("12345678901"));
    }

    @Test
    @DisplayName("CT46 [-] Cliente não consulta dados de outro cliente (403)")
    void clienteNaoConsultaOutro() throws Exception {
        get_("/api/clientes/" + ID_MARIA, JOAO).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT47 [+] Revogação de consentimento retira o cliente do programa de fidelidade")
    void revogacao() throws Exception {
        registrar("revoga@email.com", true);
        get_("/api/clientes/me/fidelidade", "revoga@email.com").andExpect(jsonPath("$.participa").value(true));
        post_("/api/clientes/me/consentimentos", "revoga@email.com", Map.of("finalidade", "PROGRAMA_FIDELIDADE", "concedido", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].finalidade").value("PROGRAMA_FIDELIDADE"))
                .andExpect(jsonPath("$[0].concedido").value(false));
        get_("/api/clientes/me/fidelidade", "revoga@email.com").andExpect(jsonPath("$.participa").value(false));
    }

    @Test
    @DisplayName("CT48 [+] Anonimização substitui dados pessoais e bloqueia novo login")
    void anonimizacao() throws Exception {
        String email = "apagar@email.com";
        Number id = ler(registrar(email, true), "$.usuarioId");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/clientes/me")
                        .header("Authorization", "Bearer " + token(email)))
                .andExpect(status().isNoContent());
        post_("/api/auth/login", null, Map.of("email", email, "senha", SENHA)).andExpect(status().isUnauthorized());
        get_("/api/clientes/" + id, MATRIZ)
                .andExpect(jsonPath("$.anonimizado").value(true))
                .andExpect(jsonPath("$.nome", startsWith("Cliente anonimizado")))
                .andExpect(jsonPath("$.cpf").value(nullValue()))
                .andExpect(jsonPath("$.email", not(containsString("apagar"))));
    }

    @Test
    @DisplayName("CT49 [+] Matriz vê vendas consolidadas por unidade e canal")
    void relatorioMatriz() throws Exception {
        criarPedidoPago();
        get_("/api/relatorios/vendas?inicio=2000-01-01&fim=2100-12-31", MATRIZ)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.unidadeId==1 && @.canal=='APP')].quantidadePedidos", hasItem(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("CT50 [-] Gerente não vê vendas de outra unidade e atendente não acessa relatórios (403)")
    void relatorioEscopo() throws Exception {
        get_("/api/relatorios/vendas?unidadeId=2", GERENTE_RECIFE).andExpect(status().isForbidden());
        get_("/api/relatorios/vendas", ATENDENTE_RECIFE).andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions registrar(String email, boolean fidelidade) throws Exception {
        return post_("/api/auth/registro", null, Map.of("nome", "Titular Teste", "email", email, "senha", SENHA,
                        "cpf", "98765432100", "aceiteTermosUso", true,
                        "consentimentos", Map.of("PROGRAMA_FIDELIDADE", fidelidade)))
                .andExpect(status().isCreated());
    }
}
