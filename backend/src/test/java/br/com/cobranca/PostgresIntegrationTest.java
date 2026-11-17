package br.com.cobranca;

import java.time.LocalDate;

import br.com.cobranca.aluno.AlunoRepository;
import br.com.cobranca.cobranca.CobrancaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;

/** Base dos testes de integração: Postgres de verdade (mesmo banco da produção) num container. */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    // Um único container para TODA a execução dos testes (padrão "singleton container"): o Spring guarda
    // contextos em cache entre classes de teste, e eles precisam continuar apontando para o mesmo banco.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcClient jdbc;
    @Autowired
    protected AlunoRepository alunos;
    @Autowired
    protected CobrancaRepository cobrancas;

    /** Estado limpo a cada teste (a escola 1 vem da migração e é mantida). */
    @BeforeEach
    void limparDados() {
        for (String t : new String[] {"webhook_evento", "importacao", "cobranca", "aluno", "turma"}) {
            jdbc.sql("DELETE FROM " + t).update();
        }
    }

    /** Cria turma, aluno e uma mensalidade ABERTA; devolve o id da cobrança. */
    protected long criarCobranca(String nomeAluno, LocalDate vencimento) {
        long turma = alunos.turmaIdPorNome(1, "Futsal Sub-11");
        long aluno = alunos.salvar(1, turma, nomeAluno, 12000, 10, "5583900000000").id();
        cobrancas.criarSeAusente(1, aluno, vencimento.withDayOfMonth(1), 12000, vencimento);
        return jdbc.sql("SELECT id FROM cobranca WHERE aluno_id = :a").param("a", aluno).query(Long.class).single();
    }

    /** Mensalidade ABERTA que já tem Pix gerado no Asaas (com o id de pagamento informado). */
    protected long criarCobrancaComPix(String nomeAluno, String asaasPaymentId) {
        long id = criarCobranca(nomeAluno, LocalDate.now().plusDays(5));
        cobrancas.salvarPixSeAusente(id, asaasPaymentId, "payload-pix", "qr-base64", "https://sandbox.asaas.com/i/x");
        return id;
    }

    protected String statusDe(long cobrancaId) {
        return jdbc.sql("SELECT status FROM cobranca WHERE id = :id").param("id", cobrancaId)
                .query(String.class).single();
    }

    protected java.time.OffsetDateTime pagoEmDe(long cobrancaId) {
        return jdbc.sql("SELECT pago_em FROM cobranca WHERE id = :id").param("id", cobrancaId)
                .query((rs, n) -> rs.getObject(1, java.time.OffsetDateTime.class)).single();
    }
}
