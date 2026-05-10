# Decisões Técnicas

Este documento descreve as principais escolhas de design e arquitetura do projeto, com a justificativa de cada uma.

---

## 1. Armazenamento do número de cartão via HMAC-SHA256

**Decisão:** O PAN (Primary Account Number) nunca é armazenado em texto puro. Antes de persistir, o número é normalizado (remoção de espaços e hífens) e transformado em um hash HMAC-SHA256 com uma chave secreta de no mínimo 32 bytes.

**Razão:** O desafio exige que dados sensíveis sejam armazenados de forma segura. Criptografia simétrica (AES, por exemplo) permitiria recuperar o número original — o que não é necessário, já que o caso de uso é apenas verificar se um cartão existe. O HMAC resolve isso: é determinístico (permite busca por igualdade), irreversível sem a chave, e resistente a ataques de dicionário pelo fator da chave secreta.

---

## 2. Autenticação via JWT

**Decisão:** Autenticação stateless com JWT assinado em HS256. O token é gerado no login e validado em cada requisição via filtro de segurança.

**Razão:** O desafio permite JWT ou OAuth2. JWT foi escolhido pela simplicidade de implementação e por não exigir infraestrutura adicional (servidor de autorização). A abordagem stateless também favorece a escalabilidade horizontal da aplicação.

---

## 3. Endpoint de consulta como POST

**Decisão:** A busca de cartão é feita via `POST /cards/search` com o número no corpo da requisição, não via `GET /cards/{number}`.

**Razão:** Um `GET` com o PAN na URL o exporia em logs de servidor, proxies e histórico de browser. Com POST e corpo JSON, o número trafega apenas no payload HTTPS e não aparece em access logs ou em qualquer cache de infraestrutura.

---

## 4. Transações independentes por linha no batch

**Decisão:** O processamento do arquivo em lote usa `Propagation.NOT_SUPPORTED` no serviço orquestrador e `Propagation.REQUIRES_NEW` por linha no processador individual. Além disso, `CardServiceImpl.createFromBatch` é anotado com `@Transactional(noRollbackFor = {DuplicateCardException.class, InvalidCardException.class})`.

**Razão:** O desafio menciona que o sistema pode receber grandes volumes de dados. Processar todas as linhas em uma única transação criaria um lock prolongado no banco e tornaria tudo atômico — uma linha inválida reverteria as demais. Com transações independentes, cada cartão é confirmado ou descartado de forma isolada, e a resposta detalha o resultado de cada linha.

O `noRollbackFor` é necessário por uma característica do Spring: quando um método `@Transactional(REQUIRED)` lança uma `RuntimeException`, o proxy do Spring marca a transação corrente como rollback-only antes de propagar a exceção — mesmo que o chamador vá capturá-la. Sem esse atributo, `CardBatchLineProcessor.process()` capturaria `DuplicateCardException` normalmente, mas ao tentar fazer o commit da transação `REQUIRES_NEW` receberia `UnexpectedRollbackException`, pois a transação já estava marcada para rollback. Como `DuplicateCardException` e `InvalidCardException` são resultados de negócio esperados (não falhas de infraestrutura), marcar a transação para rollback por causa delas é incorreto.

---

## 5. Validação Luhn desabilitada por padrão

**Decisão:** A validação do algoritmo de Luhn é configurável via variável de ambiente e vem desabilitada por padrão (`CARD_VALIDATION_LUHN_ENABLED=false`).

**Razão:** O arquivo de exemplo do desafio contém números que não passam no checksum de Luhn — o objetivo é testar o processamento do formato, não a validade dos cartões. Desabilitar por padrão garante que o arquivo do desafio seja processado sem rejeições inesperadas, enquanto a flag permite ativar a validação em ambientes que exijam números reais.

---

## 6. Mascaramento de PAN nos logs

**Decisão:** Um `MessageConverter` customizado do Logback (`CardMaskingConverter`) é aplicado globalmente a todas as mensagens de log, substituindo automaticamente padrões numéricos de 13 a 19 dígitos pelo formato `BIN******últimos4`.

**Razão:** O desafio exige logging das requisições. Qualquer log que contenha acidentalmente um número de cartão (por exemplo, em uma mensagem de erro) seria uma exposição de dado sensível. O mascaramento automático no nível do appender garante que nenhum PAN chegue ao destino do log, independente de onde ele apareça no código.

---

## 7. TLS na infraestrutura, não na aplicação

**Decisão:** A aplicação não configura SSL/TLS diretamente. Em produção, o tráfego HTTPS seria terminado no proxy reverso ou load balancer antes de chegar à aplicação.

**Razão:** O desafio lista criptografia de tráfego como requisito opcional. Configurar TLS na aplicação com certificado autoassinado adicionaria fricção para quem avalia o projeto (necessidade de `curl -k`, alertas no browser) sem agregar segurança real. O padrão da indústria é terminar TLS na borda da infraestrutura (nginx, AWS ALB, etc.), mantendo a aplicação desacoplada da gestão de certificados.

---

## 8. Fail-fast na inicialização para secrets obrigatórios

**Decisão:** `JWT_SECRET` e `CARD_HMAC_KEY` são validados no momento em que os beans são criados. Se ausentes ou com menos de 32 bytes, a aplicação falha imediatamente com mensagem clara.

**Razão:** Permitir que a aplicação suba sem as chaves configuradas criaria um estado silenciosamente inseguro — tokens assinados com chave fraca ou hashes previsíveis. Falhar rápido e explicitamente força a configuração correta antes que qualquer dado seja processado.

---

## 9. Schema gerenciado pelo Flyway com `ddl-auto: validate`

**Decisão:** As migrations de banco são versionadas com Flyway. O Hibernate está configurado com `ddl-auto: validate` — apenas valida que o schema existente corresponde às entidades, sem criar ou alterar tabelas.

**Razão:** `ddl-auto: create` ou `update` em produção é arriscado — pode destruir dados ou aplicar alterações não revisadas. Com Flyway, cada mudança de schema é um arquivo versionado, revisável e rastreável. O `validate` garante que o código e o banco estão sempre em sincronia sem permitir mudanças automáticas não intencionais.
