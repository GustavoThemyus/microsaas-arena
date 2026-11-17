package br.com.cobranca.asaas;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Endpoint público chamado pelo Asaas. Autenticado pelo header {@code asaas-access-token}. */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService service;
    private final ObjectMapper json;
    private final byte[] tokenEsperado;

    public WebhookController(WebhookService service, ObjectMapper json, AsaasProperties props) {
        this.service = service;
        this.json = json;
        this.tokenEsperado = props.webhookToken() == null ? new byte[0]
                : props.webhookToken().getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/asaas")
    Map<String, String> receber(@RequestHeader(value = "asaas-access-token", required = false) String token,
                                @RequestBody String corpo) {
        // Sem token configurado, recusa tudo: melhor falhar fechado do que aceitar qualquer um.
        byte[] recebido = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        if (tokenEsperado.length == 0 || !MessageDigest.isEqual(tokenEsperado, recebido)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do webhook inválido");
        }

        JsonNode raiz;
        try {
            raiz = json.readTree(corpo);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON inválido");
        }
        String eventoId = texto(raiz, "id");
        String tipo = texto(raiz, "event");
        if (eventoId == null || tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento sem id ou tipo");
        }
        String paymentId = raiz.path("payment").path("id").isTextual() ? raiz.path("payment").path("id").asText() : null;

        var resultado = service.processar(eventoId, tipo, paymentId, corpo);
        // Sempre 200 para evento válido (inclusive repetido/ignorado): o Asaas pausa a fila após falhas.
        return Map.of("status", resultado.name());
    }

    private static String texto(JsonNode n, String campo) {
        return n.path(campo).isTextual() && !n.path(campo).asText().isBlank() ? n.path(campo).asText() : null;
    }
}
