package br.com.cobranca.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import br.com.cobranca.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test") // sem o profile "demo": nada de seed automático
class CsvImportServiceTest extends PostgresIntegrationTest {

    @Autowired
    CsvImportService service;

    private ImportacaoResultado importar(String csv) {
        return service.importar(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "alunos.csv");
    }

    private int contar(String tabela) {
        return jdbc.sql("SELECT count(*) FROM " + tabela).query(Integer.class).single();
    }

    @Test
    void importaLinhasValidasECriaAlunoTurmaECobranca() {
        var r = importar("""
                nome,turma,valor,vencimento
                Ana Souza,Futsal Sub-11,"150,00",10
                Bruno Lima,Vôlei Infantil,130,15/11/2026
                Carla Dias,Futsal Sub-11,99.90,2026-11-20
                """);

        assertThat(r.criados()).isEqualTo(3);
        assertThat(r.atualizados()).isZero();
        assertThat(r.erros()).isEmpty();
        assertThat(contar("aluno")).isEqualTo(3);
        assertThat(contar("turma")).isEqualTo(2); // turma repetida não duplica
        assertThat(contar("cobranca")).isEqualTo(3);
        assertThat(jdbc.sql("SELECT valor_centavos FROM cobranca c JOIN aluno a ON a.id = c.aluno_id WHERE a.nome = 'Ana Souza'")
                .query(Long.class).single()).isEqualTo(15000L);
        assertThat(jdbc.sql("SELECT count(*) FROM aluno WHERE length(cpf_cnpj_ficticio) = 11").query(Integer.class).single())
                .isEqualTo(3);
    }

    @Test
    void linhaRuimVaiParaORelatorioEAsBoasSaoImportadas() {
        var r = importar("""
                nome,turma,valor,vencimento
                Ana Souza,Futsal Sub-11,150,10
                ,Futsal Sub-11,150,10
                Bruno Lima,Vôlei Infantil,abc,10
                Carla Dias,Vôlei Infantil,100,31/02/2026
                Davi Rocha,Vôlei Infantil,100,12
                """);

        assertThat(r.criados()).isEqualTo(2);
        assertThat(r.erros()).extracting(ImportacaoResultado.Erro::linha).containsExactly(3, 4, 5);
        assertThat(r.erros().get(0).motivo()).contains("nome está vazio");
        assertThat(r.erros().get(1).motivo()).contains("valor inválido");
        assertThat(r.erros().get(2).motivo()).contains("vencimento inválido");
        assertThat(contar("aluno")).isEqualTo(2);
    }

    @Test
    void aceitaPontoEVirgulaDoExcelEBomEAcentosNoCabecalho() {
        var r = importar("﻿Nome;Turma;Valor;Vencimento\nAna Souza;Natação Kids;160,00;10\n");

        assertThat(r.criados()).isEqualTo(1);
        assertThat(r.erros()).isEmpty();
    }

    @Test
    void reimportarOMesmoArquivoNaoDuplicaNada() {
        String csv = "nome,turma,valor,vencimento\nAna Souza,Futsal Sub-11,150,10\n";
        importar(csv);
        var segunda = importar(csv);

        assertThat(segunda.criados()).isZero();
        assertThat(segunda.atualizados()).isEqualTo(1);
        assertThat(contar("aluno")).isEqualTo(1);
        assertThat(contar("cobranca")).isEqualTo(1);
    }

    @Test
    void cabecalhoSemColunaObrigatoriaRejeitaOArquivoTodo() {
        assertThatThrownBy(() -> importar("nome,turma,valor\nAna,Futsal,100\n"))
                .isInstanceOf(ImportacaoInvalidaException.class)
                .hasMessageContaining("vencimento");
        assertThat(contar("aluno")).isZero();
    }

    @Test
    void arquivoSoComCabecalhoOuVazioEhRejeitado() {
        assertThatThrownBy(() -> importar("nome,turma,valor,vencimento\n"))
                .isInstanceOf(ImportacaoInvalidaException.class);
        assertThatThrownBy(() -> importar("")).isInstanceOf(ImportacaoInvalidaException.class);
    }

    @Test
    void registraResumoDaImportacao() {
        importar("nome,turma,valor,vencimento\nAna,Futsal,150,10\nBruno,Futsal,x,10\n");

        var linha = jdbc.sql("SELECT linhas_ok, linhas_erro FROM importacao")
                .query((rs, n) -> rs.getInt(1) * 10 + rs.getInt(2)).single();
        assertThat(linha).isEqualTo(11);
    }
}
