# Serviço de Remessa Internacional

Desafio técnico backend: uma API que permite remessa internacional de dinheiro entre
usuários Pessoa Física (PF) e Pessoa Jurídica (PJ). Uma remessa converte um valor de
Real (BRL) para Dólar (USD) usando a cotação do Banco Central e transfere o valor
convertido para a carteira em USD do destinatário.

> **Status atual:** Incremento 1 — setup do projeto, domínio de usuários
> (PF/PJ) e carteiras. Cotação e operação de remessa serão implementadas
> nos próximos incrementos.

## Tecnologias

- **Java 25**
- **Micronaut 5** (`io.micronaut.platform:micronaut-parent:5.1.0`)
- **Maven** (com Maven Wrapper incluso — não é necessário ter o Maven instalado)
- **H2** (banco em memória)
- **Flyway** (migrations)
- **Micronaut Data JDBC** (persistência, sem overhead de um ORM completo)
- **Micronaut Validation** (Bean Validation)
- **Micronaut Serde (Jackson)** (serialização de DTOs)
- **jBCrypt** (hash de senha)
- **JUnit 5 + Mockito + AssertJ + Micronaut Test** (testes unitários e de integração)

## Arquitetura

Arquitetura inspirada em Clean/Hexagonal, aplicada de forma pragmática — sem
microsserviços e sem abstrações desnecessárias. Os pacotes principais em
`com.remessa`:

```
domain
├── model         → entidades e value objects (User, Wallet, Document, DocumentType, UserAccount)
├── policy        → regras de negócio parametrizáveis (Strategy: DailyLimitPolicy)
├── repository    → portas (interfaces) de persistência, sem depender de infraestrutura
├── security      → porta de hashing de senha
└── exception     → exceções de domínio

application
└── service       → casos de uso (UserService), orquestram domínio + portas

infrastructure
├── persistence   → adapters Micronaut Data JDBC (entities, repositories técnicos, adapters)
└── security      → implementação concreta do hashing (BCrypt)

interfaces
└── rest
    ├── controller → endpoints REST
    ├── dto        → contratos de entrada/saída da API (nunca expõem entidades)
    └── exception  → tratamento centralizado de erros HTTP
```

A regra de negócio (`domain`) não depende de nenhuma classe de infraestrutura. Os
`repository`/`security` em `domain` são portas; suas implementações concretas vivem em
`infrastructure` e são plugadas via injeção de dependência (Micronaut DI).

### Decisões arquiteturais

- **PF vs. PJ modelados por composição, não por herança.** Um único `User` contém um
  `Document` (value object) com um `DocumentType` (`CPF` ou `CNPJ`). O limite diário
  (R$10k para PF, R$50k para PJ) é resolvido por uma estratégia (`DailyLimitPolicy`,
  `sealed interface` com duas implementações + `DailyLimitPolicyResolver`), injetada
  automaticamente pelo Micronaut. Motivo: com Micronaut Data JDBC (mapeamento simples,
  sem sessão/ORM completo), herança de entidades exigiria estratégias de mapeamento
  mais complexas (tabela única com discriminador gerenciado manualmente); a estratégia
  evita isso mantendo uma única tabela `users`, é mais fácil de testar isoladamente
  (mock de `DailyLimitPolicy`) e é extensível: um novo tipo de usuário = uma nova
  policy, sem alterar código existente (Open/Closed).
- **Document valida CPF/CNPJ de verdade** (algoritmo de dígito verificador), não apenas
  formato/tamanho.
- **Micronaut Data JDBC em vez de Micronaut Data JPA/Hibernate.** Para este escopo
  (poucas entidades, sem necessidade de lazy loading, cache de sessão ou grafo de
  objetos complexo), JDBC oferece: startup mais rápido, sem reflexão em runtime,
  queries pré-computadas em tempo de compilação e uma API mais simples/previsível.
  Isso reduz a superfície de "mágica" do ORM e facilita testar e raciocinar sobre o
  SQL gerado.
- **Flyway para migrations** em vez de `schema-generate` automático do Micronaut Data —
  abordagem profissional padrão de mercado, versionada e auditável.
- **Tratamento de erros centralizado**: exceções de domínio (`DomainException` e
  subclasses) nunca vazam como stack trace; um `ExceptionHandler` único as traduz em
  respostas HTTP consistentes (`ErrorResponse`) com o status apropriado (409 para
  duplicidade, 404 para não encontrado, 400 para dados inválidos).
- **Senha nunca em texto puro**: hashing via BCrypt (jBCrypt), atrás de uma porta
  (`PasswordHasher`) para não acoplar o domínio a uma biblioteca específica.
- **BigDecimal para todo valor monetário** (nunca `double`/`float`); `java.time` para
  datas/horários.

## Modelagem do banco (Incremento 1)

```
users
├── id (UUID, PK)
├── full_name
├── email (UNIQUE)
├── password_hash
├── document_type (CPF | CNPJ)
├── document_value (UNIQUE)
└── created_at

wallets
├── id (UUID, PK)
├── user_id (UNIQUE, FK -> users.id)
├── balance_brl (NUMERIC(19,2), default 0)
├── balance_usd (NUMERIC(19,2), default 0)
└── created_at
```

Toda criação de usuário cria automaticamente uma carteira zerada, na mesma transação
(`UserServiceImpl`, `@Transactional`). Tabelas de `transfers` e `exchange_rate` serão
adicionadas em uma migration própria quando a remessa for implementada (ver
[Progresso incremental](#progresso-incremental)).

## API (Incremento 1)

| Método | Rota              | Descrição                                          |
|--------|-------------------|-----------------------------------------------------|
| POST   | `/api/users/pf`   | Cria um usuário Pessoa Física                       |
| POST   | `/api/users/pj`   | Cria um usuário Pessoa Jurídica                     |
| GET    | `/api/users/{id}` | Consulta um usuário por id, incluindo saldos        |

Exemplo de criação de PF:

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

Resposta (`201 Created`):

```json
{
  "id": "…",
  "fullName": "Ana Silva",
  "email": "ana@example.com",
  "documentType": "CPF",
  "documentValue": "52998224725",
  "balanceBrl": 0.00,
  "balanceUsd": 0.00,
  "createdAt": "…"
}
```

Erros de negócio (e-mail/documento duplicado, usuário não encontrado, documento
inválido) retornam um corpo JSON padronizado, por exemplo:

```json
{
  "timestamp": "2026-08-13T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Já existe um usuário cadastrado com o e-mail: ana@example.com",
  "path": "/api/users/pf"
}
```

## Como compilar e executar

Não é necessário ter o Maven instalado — o projeto inclui o Maven Wrapper.

```bash
# Linux/macOS
./mvnw compile
./mvnw mn:run

# Windows
mvnw.cmd compile
mvnw.cmd mn:run
```

A aplicação sobe em `http://localhost:8080`. O banco H2 em memória e as tabelas
(via Flyway) são criados automaticamente na inicialização.

## Como testar

```bash
# Linux/macOS
./mvnw test

# Windows
mvnw.cmd test
```

A suíte cobre:

- **Testes unitários de domínio** (`DocumentTest`, `DailyLimitPolicyResolverTest`):
  validação de CPF/CNPJ e resolução de limites diários.
- **Testes unitários de serviço** (`UserServiceImplTest`): regras de criação de
  usuário (unicidade de e-mail/documento, criação automática da carteira zerada),
  usando Mockito para isolar a camada de serviço da persistência.
- **Testes de integração** (`UserControllerTest`, `@MicronautTest`): sobem o
  contexto completo (servidor embarcado + H2 + Flyway) e exercitam a API via HTTP,
  cobrindo criação de PF/PJ, consulta por id, conflito de e-mail duplicado e usuário
  não encontrado.

## Progresso incremental

O desenvolvimento segue de forma incremental, conforme solicitado no desafio.

**Feito (Incremento 1):**
- Estrutura do projeto Micronaut + Maven, com Maven Wrapper.
- Banco em memória (H2) + migrations (Flyway).
- Domínio de usuários (PF/PJ via `Document`/`DocumentType`) e carteiras
  (`Wallet`), com validação real de CPF/CNPJ e unicidade de e-mail/documento.
- Estratégia de limite diário (`DailyLimitPolicy`) modelada e testada, mas ainda
  **não conectada** ao fluxo de criação (será usada quando a remessa existir).
- Endpoints REST de criação (PF/PJ) e consulta de usuário, com DTOs próprios e
  tratamento de erro centralizado.
- Testes unitários e de integração.

**Deliberadamente fora deste incremento (planejado para os próximos):**
- Integração com a API do Banco Central (cotação `cotacaoCompra`).
- Operação de remessa (conversão BRL→USD + transferência), validação de saldo e
  do limite diário transacionado.
- Fallback de cotação para finais de semana (por indicação explícita do
  enunciado, deve ser o **último** recurso a ser implementado).
- Cache, Docker/Kubernetes (diferenciais).
