package br.com.cobranca.config;

import java.util.Map;

import br.com.cobranca.asaas.AsaasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrosApi {

    private static final Logger log = LoggerFactory.getLogger(ErrosApi.class);

    /** Falha no Asaas vira 502 com mensagem legível (e o detalhe técnico vai só para o log). */
    @ExceptionHandler(AsaasException.class)
    ResponseEntity<Map<String, String>> asaas(AsaasException e) {
        log.warn("Falha no Asaas: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("erro", e.getMessage()));
    }
}
