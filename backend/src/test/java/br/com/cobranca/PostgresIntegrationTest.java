package br.com.cobranca;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Base dos testes de integração: Postgres de verdade (mesmo banco da produção) num container. */
@SpringBootTest
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected JdbcClient jdbc;

    /** Estado limpo a cada teste (a escola 1 vem da migração e é mantida). */
    @BeforeEach
    void limparDados() {
        for (String t : new String[] {"webhook_evento", "importacao", "cobranca", "aluno", "turma"}) {
            jdbc.sql("DELETE FROM " + t).update();
        }
    }
}
