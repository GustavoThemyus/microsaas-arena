package br.com.cobranca.asaas;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciais do Asaas. Vêm de variáveis de ambiente (ASAAS_*); nunca ficam no repositório. */
@ConfigurationProperties("app.asaas")
public record AsaasProperties(String baseUrl, String apiKey, String webhookToken) {
}
