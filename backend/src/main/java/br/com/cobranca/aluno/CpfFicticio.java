package br.com.cobranca.aluno;

/**
 * Gera CPFs com dígitos verificadores válidos, mas FICTÍCIOS (derivados de um número qualquer).
 * O Asaas exige CPF/CNPJ válido ao criar o cliente; a demo não pode usar CPF de gente real.
 */
public final class CpfFicticio {

    private CpfFicticio() {
    }

    public static String gerar(long semente) {
        long base = Math.floorMod(semente, 1_000_000_000L);
        int[] d = new int[11];
        for (int i = 8; i >= 0; i--) {
            d[i] = (int) (base % 10);
            base /= 10;
        }
        if (todosIguais(d, 9)) {
            d[8] = (d[8] + 1) % 10; // CPFs com todos os dígitos iguais são rejeitados
        }
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder sb = new StringBuilder(11);
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }

    private static int digito(int[] d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += d[i] * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static boolean todosIguais(int[] d, int n) {
        for (int i = 1; i < n; i++) {
            if (d[i] != d[0]) {
                return false;
            }
        }
        return true;
    }
}
