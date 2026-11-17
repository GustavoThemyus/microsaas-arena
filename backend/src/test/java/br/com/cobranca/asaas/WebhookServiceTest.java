package br.com.cobranca.asaas;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cobranca.PostgresIntegrationTest;
import br.com.cobranca.asaas.WebhookService.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/** A regra de baixa por webhook, incluindo a idempotência (o ponto crítico do sistema). */
@ActiveProfiles("test")
class WebhookServiceTest extends PostgresIntegrationTest {

    @Autowired
    WebhookService webhook;

    private int eventosGravados() {
        return jdbc.sql("SELECT count(*) FROM webhook_evento").query(Integer.class).single();
    }

    private Resultado enviar(String eventoId, String tipo, String paymentId) {
        return webhook.processar(eventoId, tipo, paymentId, "{\"id\":\"" + eventoId + "\"}");
    }

    @Test
    void eventoDePagamentoDaBaixaNaCobranca() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");

        var r = enviar("evt_1", "PAYMENT_RECEIVED", "pay_1");

        assertThat(r).isEqualTo(Resultado.PROCESSADO);
        assertThat(statusDe(id)).isEqualTo("PAGA");
        assertThat(pagoEmDe(id)).isNotNull();
        assertThat(jdbc.sql("SELECT processado_em FROM webhook_evento").query(Object.class).single()).isNotNull();
    }

    @Test
    void mesmoEventoRepetidoNaoDaBaixaDuasVezes() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        enviar("evt_1", "PAYMENT_RECEIVED", "pay_1");
        var primeiraBaixa = pagoEmDe(id);

        var segundo = enviar("evt_1", "PAYMENT_RECEIVED", "pay_1");
        var terceiro = enviar("evt_1", "PAYMENT_RECEIVED", "pay_1");

        assertThat(segundo).isEqualTo(Resultado.DUPLICADO);
        assertThat(terceiro).isEqualTo(Resultado.DUPLICADO);
        assertThat(eventosGravados()).isEqualTo(1);
        assertThat(pagoEmDe(id)).isEqualTo(primeiraBaixa); // data da baixa não foi reescrita
    }

    @Test
    void eventoDiferenteParaCobrancaJaPagaNaoAlteraNada() {
        // Ex.: o Asaas manda PAYMENT_RECEIVED e depois PAYMENT_CONFIRMED (ids de evento diferentes).
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        enviar("evt_1", "PAYMENT_RECEIVED", "pay_1");
        var primeiraBaixa = pagoEmDe(id);

        var r = enviar("evt_2", "PAYMENT_CONFIRMED", "pay_1");

        assertThat(r).isEqualTo(Resultado.PROCESSADO); // evento novo é registrado...
        assertThat(eventosGravados()).isEqualTo(2);
        assertThat(statusDe(id)).isEqualTo("PAGA");
        assertThat(pagoEmDe(id)).isEqualTo(primeiraBaixa); // ...mas a baixa não se repete
    }

    @Test
    void duasThreadsComOMesmoEventoAoMesmoTempoGeramUmaBaixaSo() throws Exception {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        var largada = new java.util.concurrent.CountDownLatch(1);
        var futuros = new java.util.ArrayList<java.util.concurrent.Future<Resultado>>();
        for (int i = 0; i < 8; i++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                return enviar("evt_corrida", "PAYMENT_RECEIVED", "pay_1");
            }));
        }
        largada.countDown();
        int processados = 0;
        for (var f : futuros) {
            if (f.get() == Resultado.PROCESSADO) {
                processados++;
            }
        }
        pool.shutdown();

        assertThat(processados).isEqualTo(1);
        assertThat(eventosGravados()).isEqualTo(1);
        assertThat(statusDe(id)).isEqualTo("PAGA");
    }

    @Test
    void eventoQueNaoEPagamentoEhRegistradoMasIgnorado() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");

        var r = enviar("evt_1", "PAYMENT_CREATED", "pay_1");

        assertThat(r).isEqualTo(Resultado.IGNORADO);
        assertThat(statusDe(id)).isEqualTo("ABERTA");
        assertThat(eventosGravados()).isEqualTo(1);
    }

    @Test
    void pagamentoDesconhecidoNaoQuebraENaoAlteraNinguem() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");

        var r = enviar("evt_1", "PAYMENT_RECEIVED", "pay_de_outro_sistema");

        assertThat(r).isEqualTo(Resultado.PROCESSADO);
        assertThat(statusDe(id)).isEqualTo("ABERTA");
    }

    @Test
    void pagamentoDeUmAlunoNaoBaixaOutro() {
        long ana = criarCobrancaComPix("Ana Souza", "pay_ana");
        long bruno = criarCobrancaComPix("Bruno Lima", "pay_bruno");

        enviar("evt_1", "PAYMENT_RECEIVED", "pay_bruno");

        assertThat(statusDe(bruno)).isEqualTo("PAGA");
        assertThat(statusDe(ana)).isEqualTo("ABERTA");
    }
}
