package br.com.cobranca.cobranca;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import br.com.cobranca.aluno.AlunoRepository;
import br.com.cobranca.asaas.AsaasGateway;
import br.com.cobranca.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regra de negócio central. Não conhece HTTP do Asaas (usa a interface {@link AsaasGateway})
 * e é o ÚNICO lugar que dá baixa em cobrança: webhook e botão "simular" passam por aqui.
 */
@Service
public class CobrancaService {

    /** O que a tela de cobrança enxerga. {@code status}: PAGA, ABERTA ou ATRASADA (calculado). */
    public record Detalhe(long cobrancaId, String aluno, String turma, long valorCentavos, LocalDate vencimento,
                          String status, OffsetDateTime pagoEm, String pixCopiaECola, String pixQrBase64,
                          String linkFatura) {
    }

    private final CobrancaRepository cobrancas;
    private final AlunoRepository alunos;
    private final AsaasGateway asaas;
    private final AppProperties props;
    private final Clock clock;

    public CobrancaService(CobrancaRepository cobrancas, AlunoRepository alunos, AsaasGateway asaas,
                           AppProperties props, Clock clock) {
        this.cobrancas = cobrancas;
        this.alunos = alunos;
        this.asaas = asaas;
        this.props = props;
        this.clock = clock;
    }

    public Detalhe detalhe(long cobrancaId) {
        return paraDetalhe(buscar(cobrancaId));
    }

    /**
     * Passo 3: gera o Pix no Asaas. Idempotente: se a cobrança já tem Pix, devolve o mesmo
     * (clicar duas vezes não cria duas cobranças no Asaas).
     * Não usa transação de banco de propósito: não se segura conexão aberta durante chamada HTTP externa.
     */
    public Detalhe gerarPix(long cobrancaId) {
        CobrancaRepository.Linha c = buscar(cobrancaId);
        if ("PAGA".equals(c.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta mensalidade já está paga");
        }
        if (c.asaasPaymentId() != null) {
            return paraDetalhe(c);
        }

        String clienteId = c.asaasCustomerId();
        if (clienteId == null) {
            clienteId = asaas.criarCliente(c.aluno(), c.cpf());
            alunos.salvarClienteAsaas(c.alunoId(), clienteId);
        }
        // O Asaas não aceita vencimento no passado: para aluno atrasado, o Pix vence hoje.
        // O vencimento original continua guardado aqui, então ele segue "atrasado" até pagar.
        LocalDate hoje = LocalDate.now(clock);
        LocalDate vencimentoNoAsaas = c.vencimento().isBefore(hoje) ? hoje : c.vencimento();
        AsaasGateway.Cobranca pagamento = asaas.criarCobrancaPix(clienteId, c.valorCentavos(), vencimentoNoAsaas,
                "Mensalidade " + c.turma() + " - " + c.aluno(), "cobranca-" + c.id());
        AsaasGateway.QrPix qr = asaas.buscarQrPix(pagamento.id());

        cobrancas.salvarPixSeAusente(c.id(), pagamento.id(), qr.copiaECola(), qr.imagemBase64(),
                pagamento.invoiceUrl());
        // Se outro clique ganhou a corrida, devolve o Pix que ficou gravado.
        return paraDetalhe(buscar(cobrancaId));
    }

    /**
     * A baixa. Camada 2 da idempotência: só muda ABERTA para PAGA; se já está paga, não faz nada.
     * Cobre também dois eventos DIFERENTES para a mesma cobrança (ex.: CONFIRMED e RECEIVED).
     *
     * @return true se esta chamada foi a que deu a baixa
     */
    public boolean registrarPagamento(String asaasPaymentId) {
        return cobrancas.marcarComoPagaPeloAsaas(asaasPaymentId);
    }

    private CobrancaRepository.Linha buscar(long id) {
        return cobrancas.buscar(id, props.escolaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobrança não encontrada"));
    }

    private Detalhe paraDetalhe(CobrancaRepository.Linha c) {
        String status = "PAGA".equals(c.status()) ? "PAGA"
                : c.vencimento().isBefore(LocalDate.now(clock)) ? "ATRASADA" : "ABERTA";
        return new Detalhe(c.id(), c.aluno(), c.turma(), c.valorCentavos(), c.vencimento(), status, c.pagoEm(),
                c.pixPayload(), c.pixQrBase64(), c.linkFatura());
    }
}
