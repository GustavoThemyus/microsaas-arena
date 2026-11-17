package br.com.cobranca.cobranca;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

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

    /** Cobrança + dados do aluno, tudo que as telas e o Asaas precisam. */
    public record Linha(long id, long alunoId, String aluno, String turma, String cpf, String asaasCustomerId,
                        long valorCentavos, LocalDate vencimento, String status, OffsetDateTime pagoEm,
                        String asaasPaymentId, String pixPayload, String pixQrBase64, String linkFatura) {
    }

    public Optional<Linha> buscar(long id, long escolaId) {
        return jdbc.sql("""
                SELECT c.id, a.id AS aluno_id, a.nome, t.nome AS turma, a.cpf_cnpj_ficticio, a.asaas_customer_id,
                       c.valor_centavos, c.vencimento, c.status, c.pago_em, c.asaas_payment_id,
                       c.pix_payload, c.pix_qr_base64, c.link_fatura
                FROM cobranca c JOIN aluno a ON a.id = c.aluno_id JOIN turma t ON t.id = a.turma_id
                WHERE c.id = :id AND c.escola_id = :e""")
                .param("id", id).param("e", escolaId)
                .query((rs, n) -> new Linha(rs.getLong("id"), rs.getLong("aluno_id"), rs.getString("nome"),
                        rs.getString("turma"), rs.getString("cpf_cnpj_ficticio"),
                        rs.getString("asaas_customer_id"), rs.getLong("valor_centavos"),
                        rs.getObject("vencimento", LocalDate.class), rs.getString("status"),
                        rs.getObject("pago_em", OffsetDateTime.class), rs.getString("asaas_payment_id"),
                        rs.getString("pix_payload"), rs.getString("pix_qr_base64"), rs.getString("link_fatura")))
                .optional();
    }

    /** Guarda o Pix só se ainda não houver um (protege contra duplo clique gerando duas cobranças). */
    public boolean salvarPixSeAusente(long id, String asaasPaymentId, String payload, String qrBase64,
                                      String linkFatura) {
        return jdbc.sql("""
                UPDATE cobranca SET asaas_payment_id = :p, pix_payload = :pl, pix_qr_base64 = :qr, link_fatura = :l
                WHERE id = :id AND asaas_payment_id IS NULL AND status = 'ABERTA'""")
                .param("p", asaasPaymentId).param("pl", payload).param("qr", qrBase64).param("l", linkFatura)
                .param("id", id).update() > 0;
    }

    /**
     * A baixa em si. O UPDATE condicional (só muda se ainda ABERTA) é atômico no banco:
     * mesmo com dois eventos chegando ao mesmo tempo, só um deles altera a linha.
     */
    public boolean marcarComoPagaPeloAsaas(String asaasPaymentId) {
        return jdbc.sql("""
                UPDATE cobranca SET status = 'PAGA', pago_em = now()
                WHERE asaas_payment_id = :p AND status = 'ABERTA'""")
                .param("p", asaasPaymentId).update() > 0;
    }
}
