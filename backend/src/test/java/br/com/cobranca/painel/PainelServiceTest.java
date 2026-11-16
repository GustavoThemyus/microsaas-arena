package br.com.cobranca.painel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;

import br.com.cobranca.PostgresIntegrationTest;
import br.com.cobranca.demo.DemoSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "demo"})
class PainelServiceTest extends PostgresIntegrationTest {

    @Autowired
    PainelService painel;
    @Autowired
    DemoSeedService seed;

    @Test
    void seedGera30AlunosCom15Pagos8EmAbertoE7Atrasados() {
        seed.resetar();

        var p = painel.consultar(painel.mesAtual());

        assertThat(p.itens()).hasSize(30);
        assertThat(p.totais().pagos()).isEqualTo(15);
        assertThat(p.totais().abertos()).isEqualTo(8);
        assertThat(p.totais().atrasados()).isEqualTo(7);
        // pago + em aberto = total de todas as mensalidades do mês
        long soma = p.itens().stream().mapToLong(PainelService.Item::valorCentavos).sum();
        assertThat(p.totais().recebidoCentavos() + p.totais().emAbertoCentavos()).isEqualTo(soma);
        assertThat(p.totais().atrasadoCentavos()).isPositive().isLessThan(p.totais().emAbertoCentavos());
    }

    @Test
    void atrasadaEhCalculadaESomeQuandoPaga() {
        seed.resetar();
        long idAtrasada = painel.consultar(painel.mesAtual()).itens().stream()
                .filter(i -> i.status().equals("ATRASADA")).findFirst().orElseThrow().cobrancaId();

        jdbc.sql("UPDATE cobranca SET status = 'PAGA', pago_em = now() WHERE id = :id").param("id", idAtrasada).update();

        var depois = painel.consultar(painel.mesAtual());
        assertThat(depois.totais().atrasados()).isEqualTo(6);
        assertThat(depois.totais().pagos()).isEqualTo(16);
    }

    @Test
    void mesSemCobrancasVoltaVazio() {
        seed.resetar();

        var p = painel.consultar(YearMonth.of(2020, 1));

        assertThat(p.itens()).isEmpty();
        assertThat(p.totais().recebidoCentavos()).isZero();
    }
}
