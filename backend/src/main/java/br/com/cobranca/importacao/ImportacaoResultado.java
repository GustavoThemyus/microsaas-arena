package br.com.cobranca.importacao;

import java.util.List;

public record ImportacaoResultado(int criados, int atualizados, List<Erro> erros) {

    /** {@code linha} conta a partir de 1 incluindo o cabeçalho (igual ao número da linha na planilha). */
    public record Erro(int linha, String motivo) {
    }
}
