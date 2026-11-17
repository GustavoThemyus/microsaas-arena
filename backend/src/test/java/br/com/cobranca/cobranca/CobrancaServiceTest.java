package br.com.cobranca.cobranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import br.com.cobranca.PostgresIntegrationTest;
import br.com.cobranca.asaas.AsaasGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

@ActiveProfiles("test")
class CobrancaServiceTest extends PostgresIntegrationTest {

    @Autowired
    CobrancaService service;
    @MockitoBean
    AsaasGateway asaas;

    private void asaasResponde() {
        when(asaas.criarCliente(anyString(), anyString())).thenReturn("cus_1");
        when(asaas.criarCobrancaPix(anyString(), anyLong(), any(), anyString(), anyString()))
                .thenReturn(new AsaasGateway.Cobranca("pay_1", "PENDING", "https://sandbox.asaas.com/i/1"));
        when(asaas.buscarQrPix("pay_1")).thenReturn(new AsaasGateway.QrPix("00020101...", "iVBOR..."));
    }

    @Test
    void geraPixGuardaDadosEDevolveQr() {
        asaasResponde();
        long id = criarCobranca("Ana Souza", LocalDate.now().plusDays(3));

        var d = service.gerarPix(id);

        assertThat(d.pixCopiaECola()).isEqualTo("00020101...");
        assertThat(d.pixQrBase64()).isEqualTo("iVBOR...");
        assertThat(d.linkFatura()).contains("sandbox.asaas.com");
        assertThat(d.status()).isEqualTo("ABERTA");
        assertThat(jdbc.sql("SELECT asaas_customer_id FROM aluno").query(String.class).single()).isEqualTo("cus_1");
    }

    @Test
    void gerarDuasVezesNaoCriaSegundaCobrancaNoAsaas() {
        asaasResponde();
        long id = criarCobranca("Ana Souza", LocalDate.now().plusDays(3));

        service.gerarPix(id);
        var segunda = service.gerarPix(id);

        assertThat(segunda.pixCopiaECola()).isEqualTo("00020101...");
        verify(asaas, org.mockito.Mockito.times(1)).criarCobrancaPix(anyString(), anyLong(), any(), anyString(), anyString());
    }

    @Test
    void alunoAtrasadoRecebePixQueVenceHojeMasContinuaAtrasadoAteOPagamento() {
        asaasResponde();
        long id = criarCobranca("Ana Souza", LocalDate.now().minusDays(10));

        var d = service.gerarPix(id);

        verify(asaas).criarCobrancaPix(eq("cus_1"), eq(12000L), eq(LocalDate.now()), anyString(), anyString());
        assertThat(d.vencimento()).isEqualTo(LocalDate.now().minusDays(10)); // vencimento original preservado
        assertThat(d.status()).isEqualTo("ATRASADA");
    }

    @Test
    void naoGeraPixParaCobrancaJaPaga() {
        long id = criarCobrancaComPix("Ana Souza", "pay_1");
        service.registrarPagamento("pay_1");

        assertThatThrownBy(() -> service.gerarPix(id)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já está paga");
        verify(asaas, never()).criarCliente(anyString(), anyString());
    }

    @Test
    void cobrancaInexistenteDa404() {
        assertThatThrownBy(() -> service.detalhe(999_999)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não encontrada");
    }
}
