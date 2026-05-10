# Card Service

API REST em Java 21 + Spring Boot para cadastro e consulta segura de números de cartão de crédito.

> As decisões de arquitetura e design do projeto estão documentadas em [TECHNICAL-DECISIONS.md](TECHNICAL-DECISIONS.md).

## Stack

- Java 21 + Spring Boot 3.3.4
- MySQL 8 + Flyway (migrations)
- JWT (JJWT 0.12.6)
- Docker / Docker Compose
- Swagger UI (springdoc-openapi 2.6.0)

## Segurança

- Números de cartão armazenados como **HMAC-SHA256** com chave secreta — nunca em texto puro
- Senhas de usuário com **BCrypt** (custo 12)
- Autenticação via **JWT HS256** stateless
- Lookup via POST — PAN nunca exposto em URL ou logs de acesso
- Logging automático de todas as requisições com **mascaramento de PAN** nos logs
- Em produção, o tráfego deve ser protegido com TLS terminado no proxy reverso ou load balancer

## Endpoints

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| POST | `/auth/login` | Não | Autentica e retorna JWT |
| POST | `/cards` | JWT | Cadastra cartão único |
| POST | `/cards/batch` | JWT | Cadastra cartões via arquivo TXT |
| POST | `/cards/search` | JWT | Verifica se cartão existe e retorna seu identificador |

## Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/cardservice?...` | URL JDBC do banco |
| `DB_USERNAME` | `cardservice` | Usuário do banco |
| `DB_PASSWORD` | `cardservice` | Senha do banco |
| `JWT_SECRET` | _(obrigatório)_ | Chave HS256 para JWT — mínimo 32 bytes |
| `JWT_EXPIRATION_MS` | `3600000` | Validade do token em ms (padrão: 1 hora) |
| `CARD_HMAC_KEY` | _(obrigatório)_ | Chave HMAC-SHA256 para hash do PAN — mínimo 32 bytes |
| `CARD_VALIDATION_LUHN_ENABLED` | `false` | Habilita validação do algoritmo de Luhn |
| `SEED_USERNAME` | `admin` | Usuário criado na primeira inicialização |
| `SEED_PASSWORD` | `ChangeMe!2026` | Senha do usuário seed |

## Setup com Docker

**Pré-requisitos:** Docker, Docker Compose e Maven 3.9+

```bash
cp .env.example .env
# Edite .env — especialmente JWT_SECRET e CARD_HMAC_KEY (mínimo 32 caracteres cada)

mvn clean package -DskipTests

cd docker && docker-compose up --build
```

API disponível em `http://localhost:8080`.
Documentação interativa: `http://localhost:8080/swagger-ui.html`

**Credenciais padrão:** `admin` / `ChangeMe!2026`

## Setup local (sem Docker)

**Pré-requisitos:** Java 21, MySQL 8 rodando localmente com banco `cardservice` criado

```bash
export DB_URL=jdbc:mysql://localhost:3306/cardservice?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
export DB_USERNAME=cardservice
export DB_PASSWORD=cardservice
export JWT_SECRET=change-me-please-use-a-strong-32byte-secret-key!!
export JWT_EXPIRATION_MS=3600000
export CARD_HMAC_KEY=change-me-please-use-a-strong-32byte-hmac-key!!!
export SEED_USERNAME=admin
export SEED_PASSWORD=ChangeMe!2026
export CARD_VALIDATION_LUHN_ENABLED=false

mvn spring-boot:run
```

## Exemplos de uso

### Autenticação

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"ChangeMe!2026"}'
# {"token":"eyJ...","expiresIn":3600}
```

### Cadastrar cartão único

```bash
curl -X POST http://localhost:8080/cards \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ..." \
  -d '{"cardNumber":"4532015112830366"}'
# 201: {"id":"550e8400-...","createdAt":"2026-05-08T10:00:00"}
```

### Cadastrar via arquivo TXT

```bash
curl -X POST http://localhost:8080/cards/batch \
  -H "Authorization: Bearer eyJ..." \
  -F "file=@test-case-cards-batch.txt"
```

### Consultar cartão

```bash
curl -X POST http://localhost:8080/cards/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ..." \
  -d '{"cardNumber":"4532015112830366"}'
# {"found":true,"id":"550e8400-..."}
```

## Formato do arquivo TXT

```
CARD-SERVICE-BATCH           20180524LOTE0001000002
C1     4532015112830366
C2     5425233430109903
LOTE0001000002
```

| Linha | Posições | Conteúdo |
|---|---|---|
| Cabeçalho | 01–29 | Nome do lote |
| Cabeçalho | 30–37 | Data (YYYYMMDD) |
| Cabeçalho | 38–45 | ID do lote |
| Cabeçalho | 46–51 | Quantidade de registros |
| Cartão | 01 | Letra "C" |
| Cartão | 02–07 | Sequência no lote |
| Cartão | 08–26 | Número do cartão |
| Rodapé | 01–08 | ID do lote |
| Rodapé | 09–14 | Quantidade de registros |

Encoding: ASCII. Cada cartão é processado de forma independente — uma falha não cancela os demais.

## Testes

```bash
mvn test
```

## Escalabilidade

- Busca por hash via índice único — `O(log n)` sem descriptografia
- Aplicação stateless — escala horizontalmente atrás de load balancer
- Batch com transação independente por linha — suporta volumes grandes sem lock prolongado
