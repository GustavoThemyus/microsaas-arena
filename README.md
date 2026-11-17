# Mensalidades — demonstração de cobrança por Pix para escolinhas esportivas

> **AMBIENTE DE DEMONSTRAÇÃO — DADOS FICTÍCIOS.** Pagamentos somente no *sandbox* (teste) do Asaas. Nenhum dado real de aluno,
> nenhum dinheiro real.

Demo para mostrar a donos de escolinhas e arenas (futebol, futsal, vôlei, beach tennis, natação) de João Pessoa/PB o fluxo:

1. Importar a planilha CSV de alunos ✅
2. Painel: quem pagou, quem atrasou, quanto entrou e quanto está em aberto ✅
3. Gerar cobrança Pix (QR code + link) pelo Asaas sandbox ✅
4. Baixa automática por webhook, painel atualiza sozinho ✅
5. "Lembrar no WhatsApp" com a mensagem pronta ⏳ *(entrega 3)*

Não é o produto final: é uma ferramenta de venda e o embrião do MVP, que será definido depois dos experimentos de validação.

## Como rodar localmente

Pré-requisitos: Java 21+, Node 20+, Docker.

```bash
docker compose up -d                       # Postgres na porta 5434
cd backend && ./mvnw spring-boot:run       # API em :8080 (cria o schema e o seed sozinha)
cd frontend && cp .env.example .env.local && npm install && npm run dev   # front em :3000
```

Copie `.env.example` para `.env` na raiz e preencha `ASAAS_API_KEY` (chave do **sandbox**) e `ASAAS_WEBHOOK_TOKEN`; a API lê esse
arquivo sozinha. Local, a API fica aberta (sem `DEMO_TOKEN`). Em produção, defina `DEMO_TOKEN` e abra o front com `/?k=<token>`
(o token fica salvo no aparelho e some da URL). Variáveis: veja [`.env.example`](.env.example).

Testes (usam um Postgres real num container, precisa do Docker): `cd backend && ./mvnw test`.

## Arquitetura (resumo)

Monolito modular: **um** programa Spring Boot dividido em pacotes (`aluno`, `importacao`, `cobranca`, `painel`, `demo`, e,
na próxima entrega, `asaas` e `whatsapp`). Front Next.js (PWA instalável) falando com a API por HTTP/JSON. Banco PostgreSQL
com migrações Flyway.

## Decisões e porquês

- **Spring Boot + Java:** é a stack que vou usar profissionalmente; transações e tipagem ajudam em sistema que lida com dinheiro.
  Usei Spring Boot **3.5** porque é a linha 3.x que escolhi para o projeto (o gerador oficial já só oferece 4.x).
- **Monolito modular, sem microsserviços/CQRS:** uma pessoa, um deploy, poucas horas. Os pacotes mantêm as fronteiras
  claras para separar depois, se algum dia fizer sentido.
- **JdbcClient em vez de JPA:** o domínio é pequeno, as consultas do painel são SQL agregado, e sem JPA o programa
  sobe mais rápido e usa menos memória (importante em hospedagem gratuita). Trade-off: mais SQL escrito à mão.
- **Dinheiro em centavos (`bigint`):** evita erro de arredondamento de ponto flutuante.
- **"Atrasado" é calculado, não gravado:** status gravado são só `ABERTA` e `PAGA`. Atrasada = aberta com vencimento
  passado. Assim nenhum job precisa "virar" cobranças para atrasadas e o dado nunca fica desatualizado.
- **`escola_id` em toda tabela:** quando houver mais de um cliente, o schema já está pronto. Hoje existe uma escolinha só.
- **Importação tolerante:** linha ruim não derruba o arquivo; vai para um relatório (linha + motivo) e as boas entram.
  Reimportar o mesmo arquivo não duplica (atualiza). Aceita `;` (Excel em português) e `,`, valores `150,00`/`150.00`/`R$ 150`.
- **CPFs fictícios válidos:** o Asaas exige CPF válido ao criar cliente; geramos CPFs com dígito verificador correto a
  partir de um número, sem relação com pessoas reais.
- **Dados 100% fictícios:** a maioria dos alunos é menor de idade (LGPD). Nada real entra na demo.
- **Link de demo com token** em vez de login: não existe cadastro público; basta impedir que qualquer um na internet mexa nos dados.
- **Polling a cada 5 s no painel** em vez de WebSocket: simples e suficiente para "atualiza sozinho".
- **Tela "Acordando o sistema…":** a hospedagem gratuita da API dorme após 15 min; o front tenta `/api/health` até ela
  acordar em vez de mostrar erro.

### Pagamento: Asaas, webhook e idempotência

- **Baixa por webhook (e não por clique humano):** o banco/gateway é a fonte da verdade. O dono não precisa conferir extrato
  nem marcar "pago" à mão. O custo é que o webhook precisa ser tratado com cuidado.
- **O Asaas entrega cada evento "pelo menos uma vez"**, então o mesmo evento pode chegar repetido. Duas camadas de defesa
  (`WebhookService`, `CobrancaRepository.marcarComoPagaPeloAsaas`):
  1. o `id` do evento é gravado numa coluna `UNIQUE`; repetido = ignorado (responde 200 mesmo assim, senão o Asaas pausa a fila);
  2. a baixa é um `UPDATE ... WHERE status = 'ABERTA'` atômico: eventos *diferentes* para a mesma cobrança
     (ex.: `PAYMENT_RECEIVED` e `PAYMENT_CONFIRMED`) não dão baixa duas vezes, nem em concorrência (há teste com 8 threads).
  Tudo roda numa transação: se a baixa falhar, o evento não fica gravado e o Asaas tenta de novo.
- **Autenticação do webhook:** header `asaas-access-token` comparado em tempo constante. Sem token configurado, recusa tudo
  (falha fechada).
- **Pix usa `PAYMENT_RECEIVED`**; tratamos `PAYMENT_CONFIRMED` também como pago porque a documentação não garante que ele nunca apareça no Pix.
- **Aluno atrasado:** o Asaas não aceita vencimento no passado, então o Pix vence hoje; o vencimento original fica guardado
  e o aluno continua "atrasado" até pagar.
- **Gerar Pix é idempotente:** clicar duas vezes devolve o mesmo Pix (não cria duas cobranças). Não segura transação de banco
  durante a chamada HTTP externa.
- **Trava de segurança:** o cliente do Asaas recusa iniciar se a URL não for de sandbox.
- **Botão "Simular pagamento (só demo)":** a documentação do Asaas tem `POST /v3/sandbox/payment/{id}/confirm`
  (testado: funciona para Pix). O botão chama esse endpoint e depois passa um evento pelo **mesmo** `WebhookService` do webhook
  real. Quando o webhook real chega depois, a idempotência o descarta. Só existe com o profile `demo`.

*(Seções sobre WhatsApp assistido e deploy entram na próxima entrega.)*

## O que ficou de fora (e por quê)

Envio automático de WhatsApp, nota fiscal, controle de presença, app para aluno/responsável, cartão de crédito,
contabilidade/fluxo de caixa, torneios, uniformes, estoque, múltiplos usuários com permissões, cadastro público,
gestão de múltiplos clientes: não são necessários para o fluxo de 5 passos e só serão decididos depois da validação
com donos de escolinha. Também ficou de fora: modo offline do PWA e geração automática das mensalidades dos meses seguintes.
