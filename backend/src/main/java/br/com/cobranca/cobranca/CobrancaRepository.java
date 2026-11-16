package br.com.cobranca.cobranca;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CobrancaRepository {

    private final JdbcClient jdbc;

    public CobrancaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Cria a mensalidade do mês se ainda não existir (UNIQUE aluno+competência). */
    public boolean criarSeAusente(long escolaId, long alunoId, LocalDate competencia, long valorCentavos,
                                  LocalDate vencimento) {
        return jdbc.sql("""
                INSERT INTO cobranca (escola_id, aluno_id, competencia, valor_centavos, vencimento)
                VALUES (:e, :a, :c, :v, :venc)
                ON CONFLICT (aluno_id, competencia) DO NOTHING""")
                .param("e", escolaId).param("a", alunoId).param("c", competencia)
                .param("v", valorCentavos).param("venc", vencimento)
                .update() > 0;
    }
}
