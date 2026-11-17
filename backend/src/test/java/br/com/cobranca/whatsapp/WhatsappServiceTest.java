package br.com.cobranca.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import br.com.cobranca.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class WhatsappServiceTest extends PostgresIntegrationTest {

    @Autowired
    WhatsappService whatsapp;

    @Test
    void lembreteDeAtrasadaTemTextoEUrlCodificada() {
        long id = criarCobranca("Miguel Andrade", LocalDate.now().minusDays(5));

        WhatsappService.Mensagem m = whatsapp.lembrete(id);

        assertThat(m.telefone()).isEqualTo("5583900000000");
        assertThat(m.texto()).contains("Miguel Andrade").contains("venceu em").contains("120,00");
        assertThat(m.url()).startsWith("https://wa.me/5583900000000?text=Ol%C3%A1").doesNotContain(" ");
    }

    @Test
    void lembreteDeAVencerUsaTextoDeAviso() {
        long id = criarCobranca("Helena Albuquerque", LocalDate.now().plusDays(3));

        assertThat(whatsapp.lembrete(id).texto()).contains("vence em").doesNotContain("venceu");
    }

    @Test
    void incluiLinkDoPixQuandoJaFoiGerado() {
        long id = criarCobrancaComPix("Davi Montenegro", "pay_x");

        assertThat(whatsapp.lembrete(id).texto()).contains("https://sandbox.asaas.com/i/x");
    }

    @Test
    void naoGeraLembreteParaMensalidadePaga() {
        long id = criarCobrancaComPix("Gael Pontes", "pay_y");
        cobrancas.marcarComoPagaPeloAsaas("pay_y");

        assertThatThrownBy(() -> whatsapp.lembrete(id)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já está paga");
    }

    @Test
    void cobrancaInexistenteDa404() {
        assertThatThrownBy(() -> whatsapp.lembrete(999_999L)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não encontrada");
    }

    @Test
    void normalizaTelefones() {
        assertThat(WhatsappService.normalizarTelefone("(83) 99175-7192")).isEqualTo("5583991757192");
        assertThat(WhatsappService.normalizarTelefone("5583991757192")).isEqualTo("5583991757192");
        assertThat(WhatsappService.normalizarTelefone("123")).isNull();
        assertThat(WhatsappService.normalizarTelefone(null)).isNull();
    }
}
