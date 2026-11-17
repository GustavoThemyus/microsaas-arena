package br.com.cobranca.demo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import br.com.cobranca.asaas.AsaasGateway;
import br.com.cobranca.asaas.WebhookService;
import br.com.cobranca.cobranca.CobrancaRepository;
import br.com.cobranca.config.AppProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * EXCLUSIVO DA DEMO. Em produção o pagamento acontece quando o aluno paga o Pix no banco.
 * Aqui: (1) pede ao Asaas sandbox para simular o recebimento (o Asaas então manda o webhook de verdade);
 * (2) em seguida confirma o status no Asaas e passa um evento "sim_..." pelo MESMO {@link WebhookService}
 * que o webhook real usa. Assim a demo funciona mesmo sem URL pública, e quando o webhook real chega
 * depois, a idempotência o descarta sem dar baixa duas vezes.
 */
@Service
@Profile("demo")
public class DemoPagamentoService {

    private static final Set<String> STATUS_PAGO = Set.of("RECEIVED", "CONFIRMED", "RECEIVED_IN_CASH");

    private final CobrancaRepository cobrancas;
    private final AsaasGateway asaas;
    private final WebhookService webhook;
    private final AppProperties props;

    public DemoPagamentoService(CobrancaRepository cobrancas, AsaasGateway asaas, WebhookService webhook,
                                AppProperties props) {
        this.cobrancas = cobrancas;
        this.asaas = asaas;
        this.webhook = webhook;
        this.props = props;
    }

    public Map<String, String> simular(long cobrancaId) {
        var c = cobrancas.buscar(cobrancaId, props.escolaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobrança não encontrada"));
        if ("PAGA".equals(c.status())) {
            return Map.of("status", "JA_PAGA");
        }
        if (c.asaasPaymentId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gere o Pix antes de simular o pagamento");
        }
        asaas.confirmarPagamentoNoSandbox(c.asaasPaymentId());
        String statusNoAsaas = asaas.consultarStatus(c.asaasPaymentId());
        if (!STATUS_PAGO.contains(statusNoAsaas)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Asaas não confirmou o pagamento");
        }
        var resultado = webhook.processar("sim_" + UUID.randomUUID(), "PAYMENT_RECEIVED", c.asaasPaymentId(),
                "{\"origem\":\"simulacao-demo\",\"payment\":{\"id\":\"" + c.asaasPaymentId() + "\"}}");
        return Map.of("status", resultado.name());
    }
}
