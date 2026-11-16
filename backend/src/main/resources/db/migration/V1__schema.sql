-- Schema inicial. Toda tabela de negócio carrega escola_id (preparado para multi-cliente,
-- mas a demo usa uma única escolinha). Valores em centavos (bigint) para evitar erro de arredondamento.

CREATE TABLE escola (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE turma (
    id         BIGSERIAL PRIMARY KEY,
    escola_id  BIGINT       NOT NULL REFERENCES escola (id),
    nome       VARCHAR(80)  NOT NULL,
    modalidade VARCHAR(40),
    UNIQUE (escola_id, nome)
);

CREATE TABLE aluno (
    id                         BIGSERIAL PRIMARY KEY,
    escola_id                  BIGINT       NOT NULL REFERENCES escola (id),
    turma_id                   BIGINT       NOT NULL REFERENCES turma (id),
    nome                       VARCHAR(120) NOT NULL,
    responsavel_nome           VARCHAR(120),
    telefone                   VARCHAR(20),
    cpf_cnpj_ficticio          VARCHAR(14)  NOT NULL,
    valor_mensalidade_centavos BIGINT       NOT NULL CHECK (valor_mensalidade_centavos > 0),
    dia_vencimento             SMALLINT     NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 28),
    asaas_customer_id          VARCHAR(40),
    ativo                      BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (escola_id, turma_id, nome)
);

CREATE TABLE cobranca (
    id               BIGSERIAL PRIMARY KEY,
    escola_id        BIGINT      NOT NULL REFERENCES escola (id),
    aluno_id         BIGINT      NOT NULL REFERENCES aluno (id),
    competencia      DATE        NOT NULL,
    valor_centavos   BIGINT      NOT NULL CHECK (valor_centavos > 0),
    vencimento       DATE        NOT NULL,
    -- "Atrasada" NÃO é gravada: é ABERTA com vencimento < hoje (calculada na consulta).
    status           VARCHAR(10) NOT NULL DEFAULT 'ABERTA' CHECK (status IN ('ABERTA', 'PAGA')),
    pago_em          TIMESTAMPTZ,
    asaas_payment_id VARCHAR(40) UNIQUE,
    pix_payload      TEXT,
    pix_qr_base64    TEXT,
    link_fatura      TEXT,
    UNIQUE (aluno_id, competencia)
);
CREATE INDEX idx_cobranca_painel ON cobranca (escola_id, competencia, status, vencimento);

-- Base da idempotência do webhook: o mesmo evento do Asaas nunca é processado duas vezes.
CREATE TABLE webhook_evento (
    id              BIGSERIAL PRIMARY KEY,
    escola_id       BIGINT      NOT NULL REFERENCES escola (id),
    asaas_event_id  VARCHAR(80) NOT NULL UNIQUE,
    tipo            VARCHAR(60) NOT NULL,
    payload         JSONB       NOT NULL,
    recebido_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    processado_em   TIMESTAMPTZ
);

CREATE TABLE importacao (
    id            BIGSERIAL PRIMARY KEY,
    escola_id     BIGINT       NOT NULL REFERENCES escola (id),
    nome_arquivo  VARCHAR(200),
    linhas_ok     INT          NOT NULL,
    linhas_erro   INT          NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- A demo tem uma escolinha só (fictícia).
INSERT INTO escola (id, nome) VALUES (1, 'Escolinha Modelo (DEMO)');
SELECT setval('escola_id_seq', 1);
