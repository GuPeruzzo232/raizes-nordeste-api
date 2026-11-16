package br.com.raizesdonordeste.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autenticação, cadastro e perfis de acesso")
class AutenticacaoESegurancaTest extends TesteIntegracaoBase {

    @Test
    @DisplayName("CT01 [+] Login válido devolve token JWT com perfil e unidade")
    void loginValido() throws Exception {
        post_("/api/auth/login", null, Map.of("email", ATENDENTE_RECIFE, "senha", SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.perfil").value("ATENDENTE"))
                .andExpect(jsonPath("$.unidadeId").value(1));
    }

    @Test
    @DisplayName("CT02 [-] Login com senha errada retorna 401")
    void loginSenhaErrada() throws Exception {
        post_("/api/auth/login", null, Map.of("email", MARIA, "senha", "errada123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    @DisplayName("CT03 [-] Endpoint protegido sem token retorna 401")
    void semToken() throws Exception {
        get_("/api/pedidos", null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CT04 [-] Perfil COZINHA não acessa auditoria (403)")
    void cozinhaSemAuditoria() throws Exception {
        get_("/api/auditoria", COZINHA_RECIFE).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT05 [+] Cadastro de cliente com consentimento explícito de fidelidade")
    void cadastroComConsentimento() throws Exception {
        var corpo = Map.of("nome", "Cícero Romão", "email", "cicero@email.com", "senha", "SenhaForte1",
                "aceiteTermosUso", true, "canalOrigem", "APP",
                "consentimentos", Map.of("PROGRAMA_FIDELIDADE", true, "MARKETING_EMAIL", false));
        post_("/api/auth/registro", null, corpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));
        get_("/api/clientes/me/fidelidade", "cicero@email.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participa").value(true))
                .andExpect(jsonPath("$.pontos").value(0));
        get_("/api/clientes/me", "cicero@email.com")
                .andExpect(jsonPath("$.consentimentos", hasSize(4)))
                .andExpect(jsonPath("$.consentimentos[?(@.finalidade=='MARKETING_SMS')].concedido", contains(false)));
    }

    @Test
    @DisplayName("CT06 [-] Cadastro sem aceite dos termos retorna 422")
    void cadastroSemTermos() throws Exception {
        var corpo = Map.of("nome", "Sem Termo", "email", "semtermo@email.com", "senha", "SenhaForte1", "aceiteTermosUso", false);
        post_("/api/auth/registro", null, corpo).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("CT07 [-] Cadastro com e-mail já existente retorna 409")
    void cadastroDuplicado() throws Exception {
        var corpo = Map.of("nome", "Outra Maria", "email", MARIA, "senha", "SenhaForte1", "aceiteTermosUso", true);
        post_("/api/auth/registro", null, corpo).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CT08 [-] Cadastro com dados inválidos retorna 400 com detalhes por campo")
    void cadastroInvalido() throws Exception {
        var corpo = Map.of("nome", "", "email", "nao-e-email", "senha", "123", "aceiteTermosUso", true);
        post_("/api/auth/registro", null, corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasSize(3)));
    }
}
