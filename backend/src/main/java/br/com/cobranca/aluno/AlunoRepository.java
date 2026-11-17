package br.com.cobranca.aluno;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AlunoRepository {

    public record Salvo(long id, boolean criado) {
    }

    private final JdbcClient jdbc;

    public AlunoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long turmaIdPorNome(long escolaId, String nome) {
        return jdbc.sql("""
                INSERT INTO turma (escola_id, nome) VALUES (:e, :n)
                ON CONFLICT (escola_id, nome) DO UPDATE SET nome = EXCLUDED.nome
                RETURNING id""")
                .param("e", escolaId).param("n", nome)
                .query(Long.class).single();
    }

    /** Cria o aluno, ou atualiza valor/dia se já existir (mesmo nome na mesma turma). */
    public Salvo salvar(long escolaId, long turmaId, String nome, long valorCentavos, int diaVencimento,
                        String telefone) {
        Optional<Long> existente = jdbc.sql(
                "SELECT id FROM aluno WHERE escola_id = :e AND turma_id = :t AND nome = :n")
                .param("e", escolaId).param("t", turmaId).param("n", nome)
                .query(Long.class).optional();
        if (existente.isPresent()) {
            jdbc.sql("UPDATE aluno SET valor_mensalidade_centavos = :v, dia_vencimento = :d WHERE id = :id")
                    .param("v", valorCentavos).param("d", diaVencimento).param("id", existente.get())
                    .update();
            return new Salvo(existente.get(), false);
        }
        String cpf = CpfFicticio.gerar((escolaId + ":" + turmaId + ":" + nome).hashCode() & 0x7fffffffL);
        long id = jdbc.sql("""
                INSERT INTO aluno (escola_id, turma_id, nome, responsavel_nome, telefone, cpf_cnpj_ficticio,
                                   valor_mensalidade_centavos, dia_vencimento)
                VALUES (:e, :t, :n, :r, :tel, :cpf, :v, :d) RETURNING id""")
                .param("e", escolaId).param("t", turmaId).param("n", nome)
                .param("r", "Responsável de " + nome).param("tel", telefone).param("cpf", cpf)
                .param("v", valorCentavos).param("d", diaVencimento)
                .query(Long.class).single();
        return new Salvo(id, true);
    }

    public void salvarClienteAsaas(long alunoId, String asaasCustomerId) {
        jdbc.sql("UPDATE aluno SET asaas_customer_id = :c WHERE id = :id")
                .param("c", asaasCustomerId).param("id", alunoId).update();
    }
}
