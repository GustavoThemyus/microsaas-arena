package br.com.cobranca.importacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Regras de validação de UMA linha do CSV (sem tocar em banco, para ser fácil de testar).
 * Cada método lança {@link IllegalArgumentException} com uma mensagem legível para o dono da escolinha.
 */
final class LinhaCsv {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final long VALOR_MAXIMO_CENTAVOS = 1_000_000L; // R$ 10.000,00

    private LinhaCsv() {
    }

    static String texto(String valor, String campo, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " está vazio");
        }
        String limpo = valor.trim();
        if (limpo.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(campo + " muito longo (máx. " + tamanhoMaximo + ")");
        }
        return limpo;
    }

    /** Aceita "150", "150,00", "150.00", "1.250,50" e "R$ 150,00". Retorna centavos. */
    static long valorEmCentavos(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException("valor está vazio");
        }
        String s = bruto.trim().replace("R$", "").replace(" ", "");
        if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.'); // formato brasileiro
        }
        BigDecimal reais;
        try {
            reais = new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido: \"" + bruto.trim() + "\"");
        }
        if (reais.scale() > 2 && reais.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("valor com mais de 2 casas decimais: \"" + bruto.trim() + "\"");
        }
        long centavos = reais.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        if (centavos <= 0) {
            throw new IllegalArgumentException("valor deve ser maior que zero");
        }
        if (centavos > VALOR_MAXIMO_CENTAVOS) {
            throw new IllegalArgumentException("valor acima do limite de R$ 10.000,00");
        }
        return centavos;
    }

    /**
     * Aceita data completa (dd/MM/aaaa ou aaaa-MM-dd) ou só o dia do mês (1 a 31),
     * que vale para o mês de {@code hoje}.
     */
    static LocalDate vencimento(String bruto, LocalDate hoje) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException("vencimento está vazio");
        }
        String s = bruto.trim();
        if (s.matches("\\d{1,2}")) {
            int dia = Integer.parseInt(s);
            if (dia < 1 || dia > 31) {
                throw new IllegalArgumentException("dia de vencimento inválido: " + dia);
            }
            return YearMonth.from(hoje).atDay(Math.min(dia, YearMonth.from(hoje).lengthOfMonth()));
        }
        try {
            return s.contains("/") ? LocalDate.parse(s, BR) : LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("vencimento inválido: \"" + s + "\" (use dd/mm/aaaa)");
        }
    }
}
