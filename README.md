# Serviço de Remessa Internacional

API RESTful que permite remessa internacional entre usuários Pessoa Física (PF) e
Pessoa Jurídica (PJ). Uma remessa converte um valor em Real (BRL) para Dólar (USD)
usando a cotação de compra (`cotacaoCompra`) da API PTAX do Banco Central, debita o
valor da carteira BRL do remetente e credita o equivalente em USD na carteira do
destinatário.

## Como compilar e executar
> Configure `JAVA_HOME` para o diretório onde o JDK 25 está instalado.

Não é necessário ter o Maven instalado — o projeto inclui o Maven Wrapper.

```bash
# Linux/macOS
./mvnw mn:run

# Windows
mvnw.cmd mn:run

# Windows PowerShell
.\mvnw.cmd mn:run
```

A aplicação sobe em `http://localhost:8080`. O banco H2 em memória e as tabelas são
criados automaticamente via Flyway na inicialização.

## Como executar os testes

```bash
# Linux/macOS
./mvnw test

# Windows
mvnw.cmd test

# Windows PowerShell
.\mvnw.cmd test
```

## API

### Usuários

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/users/pf` | Cria um usuário Pessoa Física (CPF) |
| `POST` | `/api/users/pj` | Cria um usuário Pessoa Jurídica (CNPJ) |
| `GET`  | `/api/users/{id}` | Consulta usuário e saldos por id |

### Remessas

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/remessas` | Executa uma remessa BRL → USD |

### Crédito (adicional para testes)

| Método | Rota            | Descrição                                  |
|--------|-----------------|--------------------------------------------|
| `POST` | `/api/users/<uuid-do-usuario>/credit` | Adiciona saldo BRL e/ou USD para o usuário |

### Exemplos

**Criar usuário PF:**

```bash
curl -X POST http://localhost:8080/api/users/pf \
  -H "Content-Type: application/json" \
  -d '{
        "fullName": "Ana Silva",
        "email": "ana@example.com",
        "password": "secret123",
        "cpf": "529.982.247-25"
      }'
```

**Criar usuário PJ:**

```bash
curl -X POST http://localhost:8080/api/users/pj \
  -H "Content-Type: application/json" \
  -d '{
        "fullName": "Empresa LTDA",
        "email": "contato@empresa.com",
        "password": "secret123",
        "cnpj": "11.222.333/0001-81"
      }'
```

**Creditar carteira (adicional para testes):**

```bash
    curl -X POST http://localhost:8080/api/users/<uuid-do-usuario>/credit \
      -H "Content-Type: application/json" \
      -d '{
        "amountBrl": 5000.00,
        "amountUsd": 100.00
      }'
  ```

**Executar remessa:**

```bash
curl -X POST http://localhost:8080/api/remessas \
  -H "Content-Type: application/json" \
  -d '{
        "senderId": "<uuid-do-remetente>",
        "receiverId": "<uuid-do-destinatario>",
        "amountBrl": 1000.00
      }'
```

Resposta (`201 Created`):

```json
{
  "id": "...",
  "senderId": "...",
  "receiverId": "...",
  "amountBrl": 1000.00,
  "amountUsd": 165.8200,
  "exchangeRate": 6.0310,
  "executedAt": "2026-08-13T21:00:00"
}
```

**Erros retornam sempre o mesmo formato:**

```json
{
  "timestamp": "2026-08-13T21:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente para o usuário ...",
  "path": "/api/remessas"
}
```

| Situação | Status |
|----------|--------|
| E-mail ou documento duplicado | 409 |
| Usuário não encontrado | 404 |
| Documento inválido / campo obrigatório ausente | 400 |
| Saldo insuficiente | 422 |
| Limite diário excedido | 422 |
| Cotação indisponível (API BCB fora do ar) | 503 |

## Requisitos implementados

| Requisito do desafio | Status |
|----------------------|--------|
| API RESTful de remessa BRL → USD | ✅ |
| Cotação via API PTAX do Banco Central (`cotacaoCompra`) | ✅ |
| Fallback para último dia útil quando sem cotação (fins de semana/feriados) | ✅ |
| Banco em memória (H2) | ✅ |
| Carteira BRL e USD por usuário | ✅ |
| Validação de saldo antes da remessa | ✅ |
| Cadastro com nome completo, e-mail, senha, CPF (PF) ou CNPJ (PJ) | ✅ |
| E-mail, CPF e CNPJ únicos | ✅ |
| Limite diário PF: R$ 10.000 | ✅ |
| Limite diário PJ: R$ 50.000 | ✅ |
| Sem restrição de tipo entre remetente e destinatário | ✅ |
| Remessa transacional (rollback em caso de falha) | ✅ |

## Tecnologias

- **Java 25** / **Maven** (com Maven Wrapper)
- **Micronaut 5** (`micronaut-parent 5.1.0`)
- **H2** (banco em memória) + **Flyway** (migrations versionadas)
- **Micronaut Data JDBC** — sem overhead de ORM, queries geradas em compile-time
- **Micronaut Serde (Jackson)** + **Micronaut Validation**
- **jBCrypt** (hash de senha)
- **JUnit 5 + Mockito + AssertJ + Micronaut Test**

## Arquitetura

Arquitetura hexagonal aplicada de forma pragmática. Pacotes em `com.remessa`:

```
domain
├── model        → User, Wallet, Transfer, Document, DocumentType
├── policy       → DailyLimitPolicy (Strategy: PF 10k / PJ 50k)
├── gateway      → ExchangeRateGateway (porta para API do BCB)
├── repository   → portas de persistência (interfaces puras)
├── security     → PasswordHasher (porta)
└── exception    → exceções de domínio tipadas

application
└── service      → RemessaService, UserService (casos de uso)

infrastructure
├── exchange     → adapter da API PTAX (PtaxClient @Client, fallback)
├── persistence  → adapters Micronaut Data JDBC
└── security     → BCryptPasswordHasher

interfaces
└── rest
    ├── controller → RemessaController, UserController
    ├── dto        → contratos HTTP (nunca expõem entidades internas)
    └── exception  → DomainExceptionHandler (tratamento centralizado)
```

O domínio não depende de nenhuma classe de infraestrutura. As portas (`gateway`,
`repository`, `security`) são interfaces em `domain`; suas implementações concretas
vivem em `infrastructure` e são injetadas pelo Micronaut.

## Decisões de design

**PF e PJ modelados por composição, não herança.** Um único `User` contém um
`Document` (value object) com `DocumentType` (`CPF` ou `CNPJ`). O limite diário é
resolvido por `DailyLimitPolicyResolver` (Strategy), que seleciona a policy correta
pelo tipo do documento do remetente. Isso mantém uma única tabela `users`, é
extensível sem modificar código existente (Open/Closed) e é trivialmente testável.

**`LocalDateTime` em `Transfer.executedAt`** — a coluna é `TIMESTAMP` sem timezone,
e `LocalDateTime` é persistido literalmente sem conversão de fuso. Isso garante que
`CAST(executed_at AS DATE) = :date` (usado na validação do limite diário) funcione
corretamente independente do timezone da JVM, evitando que remessas próximas da
virada do dia sejam atribuídas ao dia errado.

**Cotação com fallback.** `ExchangeRateGatewayAdapter` tenta a data solicitada e
retrocede um dia por vez até `bcb.ptax.fallback-days` (padrão: 7), sem lógica
específica para sábado/domingo — qualquer ausência de cotação dispara o fallback.
`HttpClientException` interrompe imediatamente sem retroceder, pois indica problema
de conectividade, não de ausência de dado.

**`Wallet` é imutável.** `debitBrl`, `creditBrl` e `creditUsd` retornam uma nova
instância com o saldo atualizado. O `RemessaServiceImpl` obtém as novas instâncias
antes de qualquer escrita, garantindo que em caso de falha anterior ao `update()`
nenhum estado parcial é persistido.

**Validação real de CPF e CNPJ** (algoritmo de dígitos verificadores), não apenas
formato/tamanho.

**Flyway** em vez de `schema-generate` automático — versionado, auditável e o
padrão profissional de mercado.

**Idempotência no endpoint de remessa (não implementada, apenas sugestão).** Em
produção, o `POST /api/remessas` poderia aceitar um header `Idempotency-Key: <uuid>`
gerado pelo cliente. O servidor armazenaria a chave na tabela `transfers` (coluna
unique) e, em caso de retry com a mesma chave, retornaria a remessa original sem
reprocessar — evitando duplo débito em caso de timeout ou falha de rede. A
estrutura atual (transfer persistida atomicamente, porta `TransferRepository`)
suporta essa adição sem mudança de design.

## Modelagem do banco

```
users
├── id (UUID, PK)
├── full_name, email (UNIQUE), password_hash
├── document_type ('CPF' | 'CNPJ'), document_value (UNIQUE)
└── created_at

wallets
├── id (UUID, PK)
├── user_id (UNIQUE, FK → users.id)
├── balance_brl NUMERIC(19,2), balance_usd NUMERIC(19,2)
└── created_at

transfers
├── id (UUID, PK)
├── sender_id (FK → users.id), receiver_id (FK → users.id)
├── amount_brl NUMERIC(19,4), amount_usd NUMERIC(19,4)
├── exchange_rate NUMERIC(19,6)
└── executed_at TIMESTAMP
```

`NUMERIC(19,4)` para valores monetários (4 casas para precisão na conversão) e
`NUMERIC(19,6)` para a cotação. Índice em `(sender_id, executed_at)` para a query
de soma diária usada na validação do limite.

## Cobertura de testes

| Conjunto | Tipo | O que cobre |
|----------|------|-------------|
| `DocumentTest` | Unitário | Validação real de CPF/CNPJ |
| `WalletTest` | Unitário | `debitBrl`, `creditUsd`, imutabilidade, casos de borda |
| `DailyLimitPolicyResolverTest` | Unitário | Resolução de limite por tipo de documento |
| `ExchangeRateGatewayAdapterTest` | Unitário (Mockito) | Cotação, fallback, tratamento de falhas |
| `UserServiceImplTest` | Unitário (Mockito) | Criação de usuário, unicidade, erros |
| `RemessaServiceImplTest` | Unitário (Mockito) | Fluxo completo, cada validação, conversão, rollback lógico |
| `TransferRepositoryAdapterTest` | Integração (H2) | Persistência e query de soma diária |
| `PtaxClientIntegrationTest` | Integração (servidor mock) | URL OData gerada, aspas simples não encodadas |
| `UserControllerTest` | Integração (H2 + HTTP) | Endpoints de usuário ponta a ponta |
| `RemessaControllerTest` | Integração (H2 + HTTP) | Endpoint de remessa: sucesso, 404, 422, 503, 400 |
