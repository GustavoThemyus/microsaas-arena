package br.com.cobranca.asaas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AsaasRestClient implements AsaasGateway {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Erro(String code, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Resposta(String id, String status, String invoiceUrl, String encodedImage, String payload,
                    List<Erro> errors) {
    }

    private final RestClient http;

    public AsaasRestClient(AsaasProperties props) {
        if (props.baseUrl() == null || !props.baseUrl().contains("sandbox")) {
            // Trava de segurança: esta demo nunca pode apontar para o Asaas de produção.
            throw new IllegalStateException("ASAAS_BASE_URL deve ser o sandbox (https://api-sandbox.asaas.com)");
        }
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(5_000);
        timeouts.setReadTimeout(20_000);
        this.http = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(timeouts)
                .defaultHeader("access_token", props.apiKey() == null ? "" : props.apiKey())
                .defaultHeader("User-Agent", "mensalidades-demo")
                .build();
    }

    @Override
    public String criarCliente(String nome, String cpfCnpj) {
        return executar(() -> http.post().uri("/v3/customers").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", nome, "cpfCnpj", cpfCnpj)).retrieve().body(Resposta.class)).id();
    }

    @Override
    public Cobranca criarCobrancaPix(String clienteId, long valorCentavos, LocalDate vencimento, String descricao,
                                     String referenciaExterna) {
        Resposta r = executar(() -> http.post().uri("/v3/payments").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "customer", clienteId,
                        "billingType", "PIX",
                        "value", BigDecimal.valueOf(valorCentavos, 2),
                        "dueDate", vencimento.toString(),
                        "description", descricao,
                        "externalReference", referenciaExterna))
                .retrieve().body(Resposta.class));
        return new Cobranca(r.id(), r.status(), r.invoiceUrl());
    }

    @Override
    public QrPix buscarQrPix(String cobrancaId) {
        Resposta r = executar(() -> http.get().uri("/v3/payments/{id}/pixQrCode", cobrancaId).retrieve()
                .body(Resposta.class));
        return new QrPix(r.payload(), r.encodedImage());
    }

    @Override
    public void confirmarPagamentoNoSandbox(String cobrancaId) {
        executar(() -> http.post().uri("/v3/sandbox/payment/{id}/confirm", cobrancaId).retrieve()
                .body(Resposta.class));
    }

    @Override
    public String consultarStatus(String cobrancaId) {
        return executar(() -> http.get().uri("/v3/payments/{id}", cobrancaId).retrieve().body(Resposta.class))
                .status();
    }

    private <T> T executar(java.util.function.Supplier<T> chamada) {
        try {
            T r = chamada.get();
            if (r == null) {
                throw new AsaasException("Resposta vazia do Asaas");
            }
            return r;
        } catch (RestClientResponseException e) {
            // Mostra o motivo do Asaas (ex.: "CPF inválido") sem vazar cabeçalhos ou a chave.
            Resposta corpo = null;
            try {
                corpo = e.getResponseBodyAs(Resposta.class);
            } catch (RuntimeException ignorado) {
                // corpo não era JSON: cai no código HTTP
            }
            String motivo = corpo != null && corpo.errors() != null && !corpo.errors().isEmpty()
                    ? corpo.errors().get(0).description()
                    : "HTTP " + e.getStatusCode().value();
            throw new AsaasException("Asaas recusou a operação: " + motivo, e);
        } catch (AsaasException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AsaasException("Não foi possível falar com o Asaas agora", e);
        }
    }
}
