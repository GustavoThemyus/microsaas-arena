package br.com.cobranca.asaas;

/** Falha ao falar com o Asaas (rede, chave inválida, dado recusado). Vira HTTP 502 para o front. */
public class AsaasException extends RuntimeException {

    public AsaasException(String message) {
        super(message);
    }

    public AsaasException(String message, Throwable cause) {
        super(message, cause);
    }
}
