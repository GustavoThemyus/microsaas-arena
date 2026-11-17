package br.com.cobranca.asaas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cobranca.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** O endpoint HTTP do webhook: autenticação por token e resposta ao Asaas. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WebhookControllerTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static final String EVENTO = """
            {"id":"evt_abc","event":"PAYMENT_RECEIVED","dateCreated":"2026-11-16 10:00:00",
             "payment":{"id":"pay_1","status":"RECEIVED","value":120.00}}""";

    private MockHttpServletRequestBuilder req(String corpo) {
        return post("/api/webhooks/asaas").contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Test
    void semTokenOuComTokenErradoRecebe401ENaoGravaNada() throws Exception {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");

        mvc.perform(req(EVENTO)).andExpect(status().isUnauthorized());
        mvc.perform(req(EVENTO).header("asaas-access-token", "token-errado")).andExpect(status().isUnauthorized());

        assertThat(statusDe(id)).isEqualTo("ABERTA");
        assertThat(jdbc.sql("SELECT count(*) FROM webhook_evento").query(Integer.class).single()).isZero();
    }

    @Test
    void comTokenCertoDaBaixaEReenvioRetorna200SemDuplicar() throws Exception {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");

        mvc.perform(req(EVENTO).header("asaas-access-token", "token-de-teste"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PROCESSADO"));
        var baixa = pagoEmDe(id);
        mvc.perform(req(EVENTO).header("asaas-access-token", "token-de-teste"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DUPLICADO"));

        assertThat(statusDe(id)).isEqualTo("PAGA");
        assertThat(pagoEmDe(id)).isEqualTo(baixa);
        assertThat(jdbc.sql("SELECT count(*) FROM webhook_evento").query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void jsonInvalidoOuSemIdRecebe400() throws Exception {
        mvc.perform(req("isso nao e json").header("asaas-access-token", "token-de-teste"))
                .andExpect(status().isBadRequest());
        mvc.perform(req("{\"event\":\"PAYMENT_RECEIVED\"}").header("asaas-access-token", "token-de-teste"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookNaoExigeOTokenDaDemo() throws Exception {
        // Mesmo com o token de demo ligado na API, o Asaas (que não o conhece) precisa conseguir entregar.
        // O filtro da demo ignora /api/webhooks/**; aqui garantimos que a rota responde pelo token próprio.
        mvc.perform(req(EVENTO).header("asaas-access-token", "token-de-teste")).andExpect(status().isOk());
    }
}
