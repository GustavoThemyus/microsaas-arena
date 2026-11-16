package br.com.cobranca.importacao;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.cobranca.aluno.AlunoRepository;
import br.com.cobranca.cobranca.CobrancaRepository;
import br.com.cobranca.config.AppProperties;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passo 1 da demo: "migração da planilha". Colunas: nome, turma, valor, vencimento.
 * Linhas ruins não derrubam o arquivo: voltam num relatório de erros, e as boas são importadas.
 */
@Service
public class CsvImportService {

    static final int MAX_LINHAS = 500;
    private static final List<String> COLUNAS = List.of("nome", "turma", "valor", "vencimento");

    private final AlunoRepository alunos;
    private final CobrancaRepository cobrancas;
    private final JdbcClient jdbc;
    private final AppProperties props;
    private final Clock clock;

    public CsvImportService(AlunoRepository alunos, CobrancaRepository cobrancas, JdbcClient jdbc,
                            AppProperties props, Clock clock) {
        this.alunos = alunos;
        this.cobrancas = cobrancas;
        this.jdbc = jdbc;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public ImportacaoResultado importar(InputStream arquivo, String nomeArquivo) {
        String conteudo = lerTexto(arquivo);
        LocalDate hoje = LocalDate.now(clock);
        long escolaId = props.escolaId();

        int criados = 0;
        int atualizados = 0;
        List<ImportacaoResultado.Erro> erros = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectarSeparador(conteudo))
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).build()
                .parse(new StringReader(conteudo))) {

            Map<String, String> cabecalho = mapearCabecalho(parser.getHeaderMap().keySet());
            int total = 0;
            for (CSVRecord linha : parser) {
                if (++total > MAX_LINHAS) {
                    throw new ImportacaoInvalidaException("Arquivo com mais de " + MAX_LINHAS + " linhas");
                }
                int numeroLinha = (int) linha.getRecordNumber() + 1; // +1 por causa do cabeçalho
                try {
                    String nome = LinhaCsv.texto(valor(linha, cabecalho, "nome"), "nome", 120);
                    String turma = LinhaCsv.texto(valor(linha, cabecalho, "turma"), "turma", 80);
                    long centavos = LinhaCsv.valorEmCentavos(valor(linha, cabecalho, "valor"));
                    LocalDate vencimento = LinhaCsv.vencimento(valor(linha, cabecalho, "vencimento"), hoje);

                    long turmaId = alunos.turmaIdPorNome(escolaId, turma);
                    int dia = Math.min(vencimento.getDayOfMonth(), 28);
                    AlunoRepository.Salvo salvo = alunos.salvar(escolaId, turmaId, nome, centavos, dia,
                            props.demoWhatsapp());
                    cobrancas.criarSeAusente(escolaId, salvo.id(), vencimento.withDayOfMonth(1), centavos,
                            vencimento);
                    if (salvo.criado()) {
                        criados++;
                    } else {
                        atualizados++;
                    }
                } catch (IllegalArgumentException e) {
                    erros.add(new ImportacaoResultado.Erro(numeroLinha, e.getMessage()));
                }
            }
            if (total == 0) {
                throw new ImportacaoInvalidaException("O arquivo não tem nenhuma linha de aluno");
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ImportacaoInvalidaException invalida) {
                throw invalida;
            }
            if (e instanceof IllegalArgumentException || e instanceof IOException
                    || e instanceof java.io.UncheckedIOException) {
                throw new ImportacaoInvalidaException("Não foi possível ler o CSV: " + e.getMessage());
            }
            throw (RuntimeException) e;
        }

        jdbc.sql("INSERT INTO importacao (escola_id, nome_arquivo, linhas_ok, linhas_erro) VALUES (:e, :n, :ok, :er)")
                .param("e", escolaId).param("n", nomeArquivo == null ? null : nomeArquivo)
                .param("ok", criados + atualizados).param("er", erros.size()).update();
        return new ImportacaoResultado(criados, atualizados, erros);
    }

    private static String lerTexto(InputStream in) {
        try {
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return s.startsWith("﻿") ? s.substring(1) : s; // BOM que o Excel coloca
        } catch (IOException e) {
            throw new ImportacaoInvalidaException("Não foi possível ler o arquivo");
        }
    }

    /** Excel em português exporta com ";" e Google Planilhas com ",". Decide pela linha do cabeçalho. */
    static char detectarSeparador(String conteudo) {
        int fim = conteudo.indexOf('\n');
        String primeira = fim < 0 ? conteudo : conteudo.substring(0, fim);
        long ponto = primeira.chars().filter(c -> c == ';').count();
        long virgula = primeira.chars().filter(c -> c == ',').count();
        return ponto > virgula ? ';' : ',';
    }

    /** Ignora maiúsculas e acentos; devolve nome-normalizado -> nome-original da coluna. */
    private static Map<String, String> mapearCabecalho(Iterable<String> originais) {
        Map<String, String> mapa = new HashMap<>();
        for (String original : originais) {
            if (original != null) {
                mapa.put(normalizar(original), original);
            }
        }
        List<String> faltando = COLUNAS.stream().filter(c -> !mapa.containsKey(c)).toList();
        if (!faltando.isEmpty()) {
            throw new ImportacaoInvalidaException("Faltam as colunas: " + String.join(", ", faltando)
                    + ". O cabeçalho deve ter: nome, turma, valor, vencimento");
        }
        return mapa;
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private static String valor(CSVRecord linha, Map<String, String> cabecalho, String coluna) {
        String original = cabecalho.get(coluna);
        return linha.isSet(original) ? linha.get(original) : null;
    }
}
