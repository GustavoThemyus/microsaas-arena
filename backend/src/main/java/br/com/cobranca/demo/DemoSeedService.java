package br.com.cobranca.demo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.cobranca.aluno.AlunoRepository;
import br.com.cobranca.cobranca.CobrancaRepository;
import br.com.cobranca.config.AppProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escolinha fictícia com 30 alunos: 15 pagos, 8 em aberto no prazo, 7 atrasados.
 * Datas são relativas a "hoje", então a demo parece viva em qualquer dia.
 * Nomes inventados; nenhum dado real de aluno entra aqui.
 */
@Service
@Profile("demo")
public class DemoSeedService {

    private static final List<String> NOMES = List.of(
            "Miguel Andrade", "Heitor Lacerda", "Arthur Bezerra", "Davi Montenegro", "Gael Pontes",
            "Théo Cavalcanti", "Lorenzo Freire", "Bernardo Xavier", "Samuel Dantas", "Enzo Medeiros",
            "Helena Albuquerque", "Alice Nogueira", "Laura Siqueira", "Maria Clara Torres", "Sophia Lins",
            "Valentina Rangel", "Heloísa Pimentel", "Cecília Barros", "Luna Azevedo", "Manuela Queiroz",
            "Pedro Henrique Sales", "Lucas Gabriel Ramos", "Matheus Cordeiro", "Rafael Vasconcelos",
            "Isabela Fontes", "Beatriz Loureiro", "Joaquim Seixas", "Nicolas Aragão", "Ana Júlia Meireles",
            "Antônio Brandão");

    private record Turma(String nome, String modalidade, long mensalidadeCentavos) {
    }

    // Modalidades alinhadas às escolinhas pesquisadas em João Pessoa (futebol, futevôlei, vôlei, basquete, tênis).
    private static final List<Turma> TURMAS = List.of(
            new Turma("Futebol Sub-11", "Futebol", 12000),
            new Turma("Futevôlei Iniciante", "Futevôlei", 15000),
            new Turma("Vôlei Infantil", "Vôlei", 13000),
            new Turma("Basquete Sub-13", "Basquete", 14000),
            new Turma("Tênis Iniciante", "Tênis", 18000));

    private final JdbcClient jdbc;
    private final AlunoRepository alunos;
    private final CobrancaRepository cobrancas;
    private final AppProperties props;
    private final Clock clock;

    public DemoSeedService(JdbcClient jdbc, AlunoRepository alunos, CobrancaRepository cobrancas,
                           AppProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.alunos = alunos;
        this.cobrancas = cobrancas;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public void semearSeVazio() {
        Integer total = jdbc.sql("SELECT count(*) FROM aluno WHERE escola_id = :e")
                .param("e", props.escolaId()).query(Integer.class).single();
        if (total == 0) {
            semear();
        }
    }

    /** Apaga os dados da escolinha e recria o cenário original (botão "resetar demo"). */
    @Transactional
    public void resetar() {
        long e = props.escolaId();
        for (String tabela : List.of("webhook_evento", "importacao", "cobranca", "aluno", "turma")) {
            jdbc.sql("DELETE FROM " + tabela + " WHERE escola_id = :e").param("e", e).update();
        }
        semear();
    }

    private void semear() {
        long escolaId = props.escolaId();
        LocalDate hoje = LocalDate.now(clock);
        LocalDate competencia = hoje.withDayOfMonth(1);
        OffsetDateTime agora = OffsetDateTime.now(clock);

        for (int i = 0; i < NOMES.size(); i++) {
            Turma turma = TURMAS.get(i % TURMAS.size());
            long turmaId = alunos.turmaIdPorNome(escolaId, turma.nome());
            jdbc.sql("UPDATE turma SET modalidade = :m WHERE id = :id")
                    .param("m", turma.modalidade()).param("id", turmaId).update();

            LocalDate vencimento;
            boolean paga = false;
            if (i < 15) {                        // pagos: venceram nos últimos dias
                vencimento = hoje.minusDays(2 + (i % 12));
                paga = true;
            } else if (i < 23) {                 // em aberto, no prazo
                vencimento = hoje.plusDays(1 + (i % 10));
            } else {                             // atrasados
                vencimento = hoje.minusDays(3 + (i - 23) * 3);
            }
            var salvo = alunos.salvar(escolaId, turmaId, NOMES.get(i), turma.mensalidadeCentavos(),
                    Math.min(vencimento.getDayOfMonth(), 28), props.demoWhatsapp());
            cobrancas.criarSeAusente(escolaId, salvo.id(), competencia, turma.mensalidadeCentavos(), vencimento);
            if (paga) {
                jdbc.sql("UPDATE cobranca SET status = 'PAGA', pago_em = :p WHERE aluno_id = :a AND competencia = :c")
                        .param("p", agora.minusDays(i % 5)).param("a", salvo.id()).param("c", competencia)
                        .update();
            }
        }
    }
}
