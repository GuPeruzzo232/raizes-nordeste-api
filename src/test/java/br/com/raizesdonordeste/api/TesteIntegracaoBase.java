package br.com.raizesdonordeste.api;

import br.com.raizesdonordeste.api.integration.pagamento.ClientePagamentoResiliente;
import br.com.raizesdonordeste.api.integration.pagamento.GatewayPagamentoSimulado;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: sobe a aplicação inteira (Spring + Flyway + H2 em modo PostgreSQL)
 * e exercita a API via HTTP simulado (MockMvc), com o relógio fixado em 16/11/2026.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TesteIntegracaoBase.RelogioFixo.class)
public abstract class TesteIntegracaoBase {

    public static final String SENHA = "Senha@123";
    public static final long RECIFE = 1, SALVADOR = 2;
    public static final long BAIAO = 1, TAPIOCA = 2, CUSCUZ = 3, CARNE_DE_SOL = 4, SUCO_CAJA = 5, ACARAJE = 6, CANJICA = 7;
    public static final String MATRIZ = "matriz@raizes.com.br";
    public static final String GERENTE_RECIFE = "gerente.recife@raizes.com.br";
    public static final String ATENDENTE_RECIFE = "atendente.recife@raizes.com.br";
    public static final String COZINHA_RECIFE = "cozinha.recife@raizes.com.br";
    public static final String GERENTE_SALVADOR = "gerente.salvador@raizes.com.br";
    public static final String ATENDENTE_SALVADOR = "atendente.salvador@raizes.com.br";
    public static final String MARIA = "maria@email.com";
    public static final String JOAO = "joao@email.com";
    public static final long ID_MARIA = 8;

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioDeTeste() {
            return Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Recife"));
        }
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected GatewayPagamentoSimulado gateway;
    @Autowired
    protected ClientePagamentoResiliente clientePagamento;

    @BeforeEach
    void reiniciarSimulador() {
        gateway.limpar();
        clientePagamento.getDisjuntor().reiniciar();
    }

    protected String token(String email) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "senha", SENHA))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    protected ResultActions chamar(MockHttpServletRequestBuilder req, String email, Object corpo) throws Exception {
        if (email != null) {
            req.header("Authorization", "Bearer " + token(email));
        }
        if (corpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(corpo instanceof String s ? s : json.writeValueAsString(corpo));
        }
        return mvc.perform(req);
    }

    protected ResultActions get_(String url, String email) throws Exception {
        return chamar(get(url), email, null);
    }

    protected ResultActions post_(String url, String email, Object corpo) throws Exception {
        return chamar(post(url), email, corpo);
    }

    protected ResultActions put_(String url, String email, Object corpo) throws Exception {
        return chamar(put(url), email, corpo);
    }

    protected ResultActions patch_(String url, String email, Object corpo) throws Exception {
        return chamar(patch(url), email, corpo);
    }

    protected static <T> T ler(ResultActions r, String caminho) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), caminho);
    }

    protected static String pedidoApp(long unidade, Object... itens) {
        return pedido(unidade, "APP", itens);
    }

    /** Monta o JSON de pedido: itens em pares (produtoId, quantidade). */
    protected static String pedido(long unidade, String canal, Object... itens) {
        StringBuilder sb = new StringBuilder("{\"unidadeId\":" + unidade + (canal != null ? ",\"canal\":\"" + canal + "\"" : "") + ",\"itens\":[");
        for (int i = 0; i < itens.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append("{\"produtoId\":").append(itens[i]).append(",\"quantidade\":").append(itens[i + 1]).append('}');
        }
        return sb.append("]}").toString();
    }

    /** Cria um pedido de Maria no app e devolve o id. */
    protected long criarPedidoMaria(Object... itens) throws Exception {
        Number id = ler(post_("/api/pedidos", MARIA, pedidoApp(RECIFE, itens)).andExpect(status().isCreated()), "$.id");
        return id.longValue();
    }

    /** Cria e paga (aprovado) um pedido de Maria. */
    protected long criarPedidoPago() throws Exception {
        long id = criarPedidoMaria(TAPIOCA, 1);
        post_("/api/pedidos/" + id + "/pagamentos", MARIA, Map.of("metodo", "PIX")).andExpect(status().isCreated());
        return id;
    }

    protected int estoque(long unidade, long produto) throws Exception {
        net.minidev.json.JSONArray arr = ler(get_("/api/unidades/" + unidade + "/estoque", MATRIZ), "$[?(@.produtoId==" + produto + ")].quantidade");
        return ((Number) arr.get(0)).intValue();
    }
}
