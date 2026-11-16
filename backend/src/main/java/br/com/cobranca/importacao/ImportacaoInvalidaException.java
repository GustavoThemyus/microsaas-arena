package br.com.cobranca.importacao;

/** Arquivo inteiro inválido (sem cabeçalho esperado, vazio, grande demais). Vira HTTP 400. */
public class ImportacaoInvalidaException extends RuntimeException {

    public ImportacaoInvalidaException(String message) {
        super(message);
    }
}
