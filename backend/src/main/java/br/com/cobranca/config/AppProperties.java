package br.com.cobranca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurações lidas de variáveis de ambiente (ver application.yml e .env.example). */
@ConfigurationProperties("app")
public record AppProperties(long escolaId, String allowedOrigin, String demoToken, String demoWhatsapp) {
}
