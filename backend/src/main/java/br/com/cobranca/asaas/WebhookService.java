package br.com.cobranca.asaas;

import java.util.Set;

import br.com.cobranca.cobranca.CobrancaService;
import br.com.cobranca.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passo 4: baixa automática. O Asaas entrega cada evento "pelo menos uma vez", então o mesmo evento pode
 * chegar repetido. Defesa em duas camadas:
 *  1) o id do evento é gravado com UNIQUE: repetido = ignorado;
 *  2) a baixa só altera cobrança ainda ABERTA (ver {@link CobrancaService#registrarPagamento}).
 * Tudo numa transação: se a baixa falhar, o registro do evento também some e o Asaas tenta de novo.
 */
@Service
public class WebhookService {

    public enum Resultado { PROCESSADO, DUPLICADO, IGNORADO }

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    /** Pix usa RECEIVED; a doc não garante que CONFIRMED nunca apareça, então os dois valem como pago. */
    static final Set<String> EVENTOS_DE_PAGAMENTO = Set.of("PAYMENT_RECEIVED", "PAYMENT_CONFIRMED");

    private final JdbcClient jdbc;
    private final CobrancaService cobrancas;
    private final long escolaId;

    public WebhookService(JdbcClient jdbc, CobrancaService cobrancas, AppProperties props) {
        this.jdbc = jdbc;
        this.cobrancas = cobrancas;
        this.escolaId = props.escolaId();
    }

    @Transactional
    public Resultado processar(String eventoId, String tipo, String asaasPaymentId, String payloadJson) {
        int inseridos = jdbc.sql("""
                INSERT INTO webhook_evento (escola_id, asaas_event_id, tipo, payload)
                VALUES (:e, :id, :t, CAST(:p AS jsonb))
                ON CONFLICT (asaas_event_id) DO NOTHING""")
                .param("e", escolaId).param("id", eventoId).param("t", tipo).param("p", payloadJson).update();
        if (inseridos == 0) {
            log.info("Webhook {} repetido ({}): ignorado", eventoId, tipo);
            return Resultado.DUPLICADO;
        }

        Resultado resultado = Resultado.IGNORADO;
        if (EVENTOS_DE_PAGAMENTO.contains(tipo) && asaasPaymentId != null) {
            boolean baixou = cobrancas.registrarPagamento(asaasPaymentId);
            log.info("Webhook {} ({}): pagamento {} {}", eventoId, tipo, asaasPaymentId,
                    baixou ? "baixado" : "sem alteração (já pago ou desconhecido)");
            resultado = Resultado.PROCESSADO;
        }
        jdbc.sql("UPDATE webhook_evento SET processado_em = now() WHERE asaas_event_id = :id")
                .param("id", eventoId).update();
        return resultado;
    }
}
