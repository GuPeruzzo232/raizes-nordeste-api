# Raízes do Nordeste — API Back-end

API REST única para os canais **app, totem, balcão, pick-up e web** da rede de restaurantes Raízes do Nordeste.
Projeto Multidisciplinar UNINTER — trilha Back-end.

**Stack:** Java 21 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA/Hibernate · PostgreSQL 16 · Flyway · springdoc-openapi (Swagger) · JUnit 5 + MockMvc · Postman/Newman · Docker.

## Como executar

### Opção 1 — Docker (recomendado)

```bash
docker compose up --build
```

Sobe PostgreSQL, **duas réplicas** da API e um balanceador nginx em `http://localhost:8080`.

### Opção 2 — Local

Pré-requisitos: JDK 21, Maven 3.9 e um PostgreSQL (ou só o banco via `docker compose up db`).

```bash
mvn spring-boot:run
```

Variáveis de ambiente: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SEGREDO` (≥ 32 caracteres), `WEBHOOK_SEGREDO`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

### Testes

```bash
mvn test          # 50 cenários de integração (H2 em modo PostgreSQL)
```

Coleção Postman: `postman/raizes-nordeste.postman_collection.json` (importe no Postman e rode no Runner, ou `newman run postman/raizes-nordeste.postman_collection.json`).
O GitHub Actions roda os testes JUnit **e** a coleção Postman (Newman) contra PostgreSQL a cada push; os relatórios ficam nos artefatos da execução.

## Usuários de demonstração (senha `Senha@123`)

| Perfil | E-mail | Unidade |
|---|---|---|
| MATRIZ | matriz@raizes.com.br | todas |
| GERENTE | gerente.recife@raizes.com.br | Recife (1) |
| ATENDENTE | atendente.recife@raizes.com.br | Recife (1) |
| COZINHA | cozinha.recife@raizes.com.br | Recife (1) |
| GERENTE | gerente.salvador@raizes.com.br | Salvador (2) |
| ATENDENTE | atendente.salvador@raizes.com.br | Salvador (2) |
| CLIENTE | maria@email.com | — (participa da fidelidade) |
| CLIENTE | joao@email.com | — (sem consentimentos) |

## Decisões para os pontos do caso

| Ponto | Como foi resolvido |
|---|---|
| **Multicanal** | Um único `POST /api/pedidos` para todos os canais. O campo `canal` (APP, TOTEM, BALCAO, PICKUP, WEB) é obrigatório e gravado no pedido, junto com `criado_por_id`. Header `Idempotency-Key` evita pedido duplicado quando o totem/app reenvia. |
| **Unidades, cardápio, estoque, sazonais** | `produto` é o catálogo corporativo; `item_cardapio` (unidade × produto) guarda preço local, disponibilidade e **vigência** (`inicio_vigencia`/`fim_vigencia`) para itens juninos; `estoque` (unidade × produto) guarda o saldo de cada loja. |
| **Pagamento externo** | Três fases (grava PENDENTE → chama provedor fora da transação → registra resposta). Provedor fora: pagamento fica `FALHA_COMUNICACAO`, a API devolve **202** e um agendador reenvia com a **mesma chave de idempotência** (sem cobrança dupla). Disjuntor abre após 3 falhas. Webhook repetido é ignorado pelo `evento_id` UNIQUE. |
| **Auditoria** | Tabela `registro_auditoria` (somente inserção) com usuário, perfil, unidade, ação, entidade, id, detalhes (antes/depois, motivo), IP e data/hora. Cancelamentos, descontos, ajustes de estoque, cardápio, status, pagamentos e acessos a dados pessoais. |
| **LGPD / fidelidade** | Consentimento explícito por finalidade, versionado e histórico (`consentimento`). Pontos só com consentimento vigente. Anonimização (`DELETE /api/clientes/me`) mantém pedidos sem dado pessoal. Toda consulta de funcionário a dados de cliente gera `ACESSO_DADOS_PESSOAIS`; gerente vê CPF mascarado. |
| **Pico de demanda** | API stateless (JWT) → escala horizontal (compose com 2 réplicas + nginx); baixa de estoque atômica no banco (`UPDATE ... WHERE quantidade >= :qtd`); bloqueio otimista (`@Version`); pool Hikari; índices; paginação; chamadas externas fora de transação; health checks. |
| **Perfis** | CLIENTE, ATENDENTE, COZINHA, GERENTE, MATRIZ. `@PreAuthorize` por endpoint + escopo nos serviços (cliente só os próprios pedidos; loja só a sua unidade; matriz tudo). A cozinha recebe uma visão sem dados pessoais nem valores. |

## Principais endpoints

| Método | Rota | Perfis |
|---|---|---|
| POST | `/api/auth/login` | público |
| POST | `/api/auth/registro` | público |
| GET | `/api/unidades` · `/api/unidades/{id}/cardapio?data=` | público |
| PUT | `/api/unidades/{id}/cardapio` | GERENTE, MATRIZ |
| GET/PUT | `/api/unidades/{id}/estoque[/{produtoId}]` | loja / GERENTE, MATRIZ |
| POST/GET | `/api/produtos` | MATRIZ / GERENTE, MATRIZ |
| POST | `/api/pedidos` | CLIENTE, ATENDENTE, GERENTE |
| GET | `/api/pedidos` · `/api/pedidos/{id}` | todos (com escopo) |
| PATCH | `/api/pedidos/{id}/status` | COZINHA, ATENDENTE, GERENTE |
| POST | `/api/pedidos/{id}/cancelamento` | CLIENTE, ATENDENTE, GERENTE, MATRIZ |
| POST | `/api/pedidos/{id}/desconto` | ATENDENTE (≤10%), GERENTE (≤30%), MATRIZ |
| POST/GET | `/api/pedidos/{id}/pagamentos` | CLIENTE, ATENDENTE, GERENTE |
| POST | `/api/pagamentos/webhook` | provedor (header `X-Webhook-Secret`) |
| GET | `/api/cozinha/fila` | COZINHA, GERENTE, MATRIZ |
| GET/POST/DELETE | `/api/clientes/me[...]` | CLIENTE |
| GET | `/api/clientes/{id}` | ATENDENTE, GERENTE, MATRIZ (auditado) |
| GET | `/api/auditoria` · `/api/relatorios/vendas` | GERENTE (sua unidade), MATRIZ |

## Estrutura

```
src/main/java/br/com/raizesdonordeste/api
├── config/          segurança, OpenAPI, relógio, carga inicial, agendador
├── domain/          entidades JPA e enums (StatusPedido com máquina de estados)
├── repository/      Spring Data JPA
├── service/         regras de negócio
├── integration/     porta do provedor de pagamento, simulador, disjuntor
├── security/        JWT e usuário autenticado
├── exception/       erros padronizados
└── web/             controllers REST e DTOs
src/main/resources/db/migration   esquema versionado (Flyway)
postman/                          coleção Postman
docs/                             diagramas
```
