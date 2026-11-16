package br.com.cobranca.painel;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.cobranca.config.AppProperties;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Passo 2 da demo: quem pagou, quem está atrasado, quanto entrou e quanto está em aberto. */
@Service
public class PainelService {

    public record Totais(long recebidoCentavos, long emAbertoCentavos, long atrasadoCentavos,
                         int pagos, int abertos, int atrasados) {
    }

    /** {@code status}: PAGA, ABERTA (no prazo) ou ATRASADA (aberta e vencida; calculado, nunca gravado). */
    public record Item(long cobrancaId, long alunoId, String aluno, String turma, long valorCentavos,
                       LocalDate vencimento, String status, OffsetDateTime pagoEm) {
    }

    public record Painel(String mes, Totais totais, List<Item> itens) {
    }

    private final JdbcClient jdbc;
    private final AppProperties props;
    private final Clock clock;

    public PainelService(JdbcClient jdbc, AppProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.props = props;
        this.clock = clock;
    }

    public Painel consultar(YearMonth mes) {
        LocalDate hoje = LocalDate.now(clock);
        List<Item> itens = jdbc.sql("""
                SELECT c.id AS cobranca_id, a.id AS aluno_id, a.nome AS aluno, t.nome AS turma,
                       c.valor_centavos, c.vencimento, c.pago_em,
                       CASE WHEN c.status = 'PAGA' THEN 'PAGA'
                            WHEN c.vencimento < :hoje THEN 'ATRASADA'
                            ELSE 'ABERTA' END AS status
                FROM cobranca c
                JOIN aluno a ON a.id = c.aluno_id
                JOIN turma t ON t.id = a.turma_id
                WHERE c.escola_id = :e AND c.competencia = :comp
                ORDER BY CASE WHEN c.status = 'PAGA' THEN 2
                              WHEN c.vencimento < :hoje THEN 0 ELSE 1 END,
                         c.vencimento, a.nome""")
                .param("e", props.escolaId()).param("comp", mes.atDay(1)).param("hoje", hoje)
                .query((rs, n) -> new Item(rs.getLong("cobranca_id"), rs.getLong("aluno_id"),
                        rs.getString("aluno"), rs.getString("turma"), rs.getLong("valor_centavos"),
                        rs.getObject("vencimento", LocalDate.class), rs.getString("status"),
                        rs.getObject("pago_em", OffsetDateTime.class)))
                .list();

        long recebido = 0, aberto = 0, atrasado = 0;
        int pagos = 0, abertos = 0, atrasados = 0;
        for (Item i : itens) {
            switch (i.status()) {
                case "PAGA" -> { recebido += i.valorCentavos(); pagos++; }
                case "ATRASADA" -> { atrasado += i.valorCentavos(); atrasados++; }
                default -> { aberto += i.valorCentavos(); abertos++; }
            }
        }
        // "Em aberto" = tudo que ainda não foi pago (no prazo + atrasado).
        Totais totais = new Totais(recebido, aberto + atrasado, atrasado, pagos, abertos, atrasados);
        return new Painel(mes.toString(), totais, itens);
    }

    public YearMonth mesAtual() {
        return YearMonth.now(clock);
    }
}
