package br.com.cobranca.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LinhaCsvTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 16);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "150|15000", "150,00|15000", "150.00|15000", "1.250,50|125050", "R$ 120,5|12050", " 99,99 |9999"})
    void valorEmFormatosBrasileiros(String bruto, long esperado) {
        assertThat(LinhaCsv.valorEmCentavos(bruto)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "0", "-10", "12,345", "99999,00"})
    void valorInvalidoOuForaDoLimiteEhRejeitado(String bruto) {
        assertThatThrownBy(() -> LinhaCsv.valorEmCentavos(bruto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vencimentoAceitaDataBrasileiraIsoEApenasODia() {
        assertThat(LinhaCsv.vencimento("05/11/2026", HOJE)).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(LinhaCsv.vencimento("2026-11-20", HOJE)).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(LinhaCsv.vencimento("10", HOJE)).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(LinhaCsv.vencimento("31", HOJE)).isEqualTo(LocalDate.of(2026, 11, 30)); // novembro tem 30 dias
    }

    @ParameterizedTest
    @ValueSource(strings = {"31/02/2026", "ontem", "0", "32", "2026-13-01", ""})
    void vencimentoInvalidoEhRejeitado(String bruto) {
        assertThatThrownBy(() -> LinhaCsv.vencimento(bruto, HOJE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void textoVazioOuGrandeDemaisEhRejeitado() {
        assertThatThrownBy(() -> LinhaCsv.texto("  ", "nome", 10)).hasMessageContaining("nome está vazio");
        assertThatThrownBy(() -> LinhaCsv.texto("x".repeat(11), "nome", 10)).hasMessageContaining("muito longo");
        assertThat(LinhaCsv.texto("  Ana ", "nome", 10)).isEqualTo("Ana");
    }
}
