package br.com.cobranca.whatsapp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import br.com.cobranca.cobranca.CobrancaRepository;
import br.com.cobranca.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * WhatsApp ASSISTIDO: o servidor só monta o texto e o link {@code wa.me}; quem toca em "enviar" é o dono, no
 * próprio WhatsApp. Nada é enviado automaticamente (a API oficial exige empresa verificada, templates e custo por
 * mensagem, e disparo automático arrisca bloquear o número).
 */
@Service
public class WhatsappService {

    public record Mensagem(String telefone, String texto, String url) {
    }

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CobrancaRepository cobrancas;
    private final AppProperties props;
    private final Clock clock;

    public WhatsappService(CobrancaRepository cobrancas, AppProperties props, Clock clock) {
        this.cobrancas = cobrancas;
        this.props = props;
        this.clock = clock;
    }

    public Mensagem lembrete(long cobrancaId) {
        CobrancaRepository.Linha c = cobrancas.buscar(cobrancaId, props.escolaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobrança não encontrada"));
        if ("PAGA".equals(c.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta mensalidade já está paga");
        }
        String telefone = normalizarTelefone(c.telefone());
        if (telefone == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Aluno sem telefone válido");
        }

        boolean atrasada = c.vencimento().isBefore(LocalDate.now(clock));
        String valor = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(c.valorCentavos() / 100.0)
                .replace(' ', ' ');
        StringBuilder t = new StringBuilder("Olá! ");
        if (atrasada) {
            t.append("Passando para lembrar que a mensalidade de ").append(c.aluno()).append(" (").append(c.turma())
                    .append(") de ").append(valor).append(" venceu em ").append(DATA.format(c.vencimento())).append(". ");
        } else {
            t.append("A mensalidade de ").append(c.aluno()).append(" (").append(c.turma()).append(") de ")
                    .append(valor).append(" vence em ").append(DATA.format(c.vencimento())).append(". ");
        }
        if (c.linkFatura() != null) {
            t.append("Você pode pagar por Pix neste link: ").append(c.linkFatura()).append(" ");
        } else {
            t.append("Se precisar do Pix, é só responder esta mensagem. ");
        }
        t.append("Se já pagou, desconsidere. Obrigado!");

        String texto = t.toString();
        String url = "https://wa.me/" + telefone + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return new Mensagem(telefone, texto, url);
    }

    /** Só dígitos, com DDI 55. Devolve null se não parecer um celular/fixo brasileiro. */
    static String normalizarTelefone(String bruto) {
        if (bruto == null) {
            return null;
        }
        String d = bruto.replaceAll("\\D", "");
        if (d.length() == 10 || d.length() == 11) {
            d = "55" + d;
        }
        return (d.length() == 12 || d.length() == 13) && d.startsWith("55") ? d : null;
    }
}
