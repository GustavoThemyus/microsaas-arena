package br.com.cobranca.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cobranca.PostgresIntegrationTest;
import br.com.cobranca.asaas.AsaasGateway;
import br.com.cobranca.asaas.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

@ActiveProfiles({"test", "demo"})
class DemoPagamentoServiceTest extends PostgresIntegrationTest {

    @Autowired
    DemoPagamentoService demo;
    @Autowired
    WebhookService webhook;
    @MockitoBean
    AsaasGateway asaas;

    @Test
    void simularConfirmaNoSandboxEDaBaixaPeloCaminhoDoWebhook() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        when(asaas.consultarStatus("pay_1")).thenReturn("RECEIVED");

        var r = demo.simular(id);

        verify(asaas).confirmarPagamentoNoSandbox("pay_1");
        assertThat(r.get("status")).isEqualTo("PROCESSADO");
        assertThat(statusDe(id)).isEqualTo("PAGA");
        // passou pelo mesmo registro de eventos que o webhook real usa
        assertThat(jdbc.sql("SELECT count(*) FROM webhook_evento WHERE asaas_event_id LIKE 'sim_%'")
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void webhookRealQueChegaDepoisDaSimulacaoNaoDaBaixaDeNovo() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        when(asaas.consultarStatus("pay_1")).thenReturn("RECEIVED");
        demo.simular(id);
        var baixa = pagoEmDe(id);

        webhook.processar("evt_real_do_asaas", "PAYMENT_RECEIVED", "pay_1", "{}");

        assertThat(pagoEmDe(id)).isEqualTo(baixa);
    }

    @Test
    void simularSemPixGeradoRecusa() {
        long id = criarCobranca("Ana Souza", java.time.LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> demo.simular(id)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Gere o Pix");
        verify(asaas, never()).confirmarPagamentoNoSandbox(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void simularCobrancaJaPagaNaoFazNada() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        when(asaas.consultarStatus("pay_1")).thenReturn("RECEIVED");
        demo.simular(id);

        var r = demo.simular(id);

        assertThat(r.get("status")).isEqualTo("JA_PAGA");
    }

    @Test
    void seOAsaasNaoConfirmarNaoDaBaixa() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        when(asaas.consultarStatus("pay_1")).thenReturn("PENDING");

        assertThatThrownBy(() -> demo.simular(id)).isInstanceOf(ResponseStatusException.class);
        assertThat(statusDe(id)).isEqualTo("ABERTA");
    }
}
