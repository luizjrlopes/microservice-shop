# Plano de Evolução Arquitetural — Microservice Shop

> **Status:** proposta arquitetural antes da implementação  
> **Base analisada:** `main` em `baf92ca59c162df80840e0af41c723a9f13fb16f`  
> **Objetivo:** elevar o projeto como evidência de engenharia de sistemas distribuídos sem aumentar o escopo artificialmente.

---

## 1. Decisão central

O Microservice Shop **não precisa de mais microsserviços para ficar mais forte**. O maior potencial do projeto está em transformar o fluxo pequeno que já existe em um fluxo distribuído tecnicamente consistente e demonstrável.

A evolução deve priorizar quatro capacidades:

1. **consistência entre estado e evento**;
2. **semântica de entrega e recuperação de falhas**;
3. **rastreabilidade/observabilidade do fluxo assíncrono**;
4. **provas automatizadas de que essas propriedades realmente funcionam**.

O projeto deixa de se apresentar como “laboratório para demonstrar tecnologias” e passa a ser apresentado como uma **plataforma de processamento de pedidos orientada a eventos, focada em confiabilidade distribuída**.

### Posicionamento-alvo

> **Microservice Shop é uma plataforma de processamento de pedidos orientada a eventos que demonstra consistência transacional, entrega at-least-once, processamento idempotente, recuperação de falhas e observabilidade em uma stack poliglota Java/Python.**

Esse posicionamento é mais forte do que adicionar `catalog-service`, `auth-service`, `payment-service` ou um serviço de LLM apenas para ampliar o diagrama.

---

## 2. Estado atual — o que já é bom

A base atual possui elementos que valem preservar:

- `order-service` em Java 25 + Spring Boot;
- separação entre `domain`, `application`, `infrastructure` e `interfaces`;
- `scheduler-agent` em Python consumindo RabbitMQ;
- comunicação HTTP + AMQP entre processos distintos;
- RabbitMQ e serviços executáveis via Docker Compose;
- testes unitários Java;
- testes unitários Python;
- suíte BDD em TypeScript/Cucumber;
- ADRs arquiteturais;
- experimentos reais de ML clássico para previsão de demanda e detecção de anomalias;
- Makefile e GitHub Actions já existentes.

Portanto, a estratégia não é reescrever o projeto. É **corrigir seus contratos e aprofundar a engenharia nas fronteiras em que sistemas distribuídos realmente falham**.

---

## 3. Diagnóstico

### 3.1 Confiabilidade — crítico

#### ACK mesmo quando o trabalho falha

O `scheduler-agent` atual executa a chamada HTTP dentro de `try/finally` e confirma a mensagem no `finally`. Uma indisponibilidade do `order-service`, timeout ou falha de rede pode resultar em `ACK` mesmo sem confirmação do pedido.

**Impacto:** perda silenciosa de mensagem.

#### Ausência de timeout e tratamento de status HTTP

A chamada `requests.post()` não define timeout e não exige resposta HTTP de sucesso antes do ACK.

**Impacto:** worker pode bloquear indefinidamente ou considerar falha como sucesso operacional.

#### Sem retry e DLQ

A topologia atual possui apenas exchange, routing key e fila principal.

**Impacto:** não há caminho explícito para falhas transitórias nem para mensagens que não podem ser processadas.

---

### 3.2 Consistência — crítico

#### Dual write entre banco lógico e broker

Na criação atual:

```text
salvar Order em repositório
        ↓
publicar order.created no RabbitMQ
```

As operações não são atômicas. Mesmo substituindo o repositório em memória por PostgreSQL, ainda existiria uma janela em que o pedido é persistido e o evento não é publicado.

**Decisão:** a evolução deve utilizar **Transactional Outbox**. Persistência do pedido e registro do evento entram na mesma transação de banco. Um publisher separado entrega os eventos pendentes ao RabbitMQ.

Isso aceita a possibilidade de publicação duplicada e, portanto, exige consumidor tolerante a reentrega.

---

### 3.3 Domínio/API — alto valor

O domínio atual usa `status` como string mutável e não explicita invariantes. A API também não oferece uma forma simples de observar o estado final do pedido.

Evolução necessária:

- `OrderStatus` como enum;
- validação de `productId` não vazio;
- `quantity > 0`;
- transição `PENDING -> CONFIRMED` explícita;
- confirmação repetida deve ser segura/idempotente;
- `GET /orders/{id}` para consultar o estado;
- erro de pedido inexistente mapeado para `404`;
- validações de entrada mapeadas para `400`.

**Não criar catálogo apenas para validar se um SKU existe.** Enquanto não houver catálogo real, o contrato não deve prometer `404` para produto inexistente.

---

### 3.4 Contratos de eventos — alto valor

O evento atual contém apenas dados funcionais e não possui identidade/versionamento suficientes para operação distribuída.

Envelope-alvo para `order.created.v1`:

```json
{
  "eventId": "uuid",
  "eventType": "order.created",
  "eventVersion": 1,
  "occurredAt": "2026-08-19T00:00:00Z",
  "correlationId": "uuid",
  "payload": {
    "orderId": "uuid",
    "productId": "SKU-1",
    "quantity": 2,
    "status": "PENDING"
  }
}
```

Regras:

- `eventId` é único por evento;
- `eventVersion` muda apenas com alteração incompatível;
- `correlationId` acompanha o fluxo ponta a ponta;
- mensagem AMQP deve ser persistente;
- o contrato deve existir como artefato versionado, preferencialmente AsyncAPI + JSON Schema.

---

### 3.5 Topologia RabbitMQ — alto valor

Hoje o nome `order.created` funciona simultaneamente como conceito de evento e nome de fila. Isso mistura contrato do produtor com implementação do consumidor.

Topologia-alvo:

```text
exchange: orders.events
routing key: order.created.v1

consumer scheduler-agent
├── queue: scheduler.order-created.v1
├── retry: scheduler.order-created.retry.v1
└── dlq:   scheduler.order-created.dlq.v1
```

Responsabilidades:

- produtor conhece exchange + routing key/evento;
- consumidor possui suas filas e políticas de retry/DLQ;
- produtor não precisa conhecer o nome da fila do scheduler;
- não declarar a mesma fila com argumentos diferentes em Java e Python.

Isso também permite adicionar outro consumidor no futuro sem competir pela mesma fila.

---

### 3.6 Testes — crítico

A suíte atual existe, mas não prova o comportamento distribuído completo.

Problemas encontrados:

- BDD purga e consome a mesma fila utilizada pelo worker, criando competição/race;
- cenários esperam comportamentos que o código não implementa, como produto inexistente retornar `404`;
- o CI principal não sobe a stack para executar o fluxo assíncrono real;
- os testes do worker validam inclusive o ACK em situações que deveriam ser tratadas como erro;
- não há teste da consistência entre persistência e publicação.

Pirâmide-alvo:

```text
                     E2E / BDD
              create -> event -> worker
                  -> confirmed
                       │
             Integration tests
       PostgreSQL / RabbitMQ / outbox / worker
                       │
                  Unit tests
        domínio / use cases / retry policy
```

BDD não deve “roubar” a fila do scheduler. Quando precisar observar eventos, deve usar uma **fila de auditoria exclusiva do teste**, ligada à mesma routing key.

---

### 3.7 CI — alto valor

O workflow atual instala dependências BDD com `npm install`, apesar de existir lockfile, e não executa a stack distribuída como gate.

O alvo é uma pipeline com quatro gates independentes:

1. **quality** — Spotless, Black/Ruff, TypeScript;
2. **unit-tests** — Java + Python;
3. **security** — auditoria real das dependências usadas;
4. **integration-e2e** — Docker Compose + BDD ponta a ponta.

Regras:

- `npm ci`, nunca `npm install` no CI;
- caches por ecossistema;
- logs e estado dos containers publicados como artifact quando o E2E falhar;
- nenhuma dependência de teste deve ser excluída da auditoria por estar em `devDependencies`;
- CI deve falhar quando o fluxo distribuído real falhar.

---

### 3.8 Observabilidade — alto valor, depois da confiabilidade

Não vale colocar Grafana e OpenTelemetry antes de o fluxo ser correto.

Ordem:

1. logs JSON estruturados;
2. `correlationId`, `eventId`, `orderId`, tentativa e duração nos logs;
3. métricas operacionais;
4. tracing distribuído.

Métricas úteis:

- pedidos criados/confirmados;
- eventos pendentes no outbox;
- falhas de publicação;
- mensagens processadas;
- retries;
- mensagens enviadas para DLQ;
- latência de confirmação assíncrona.

OpenTelemetry/Prometheus devem entrar em **profile opcional de observabilidade**, sem tornar o `docker compose up` básico pesado.

---

## 4. Matriz de gaps

| Prioridade | Gap | Decisão |
|---|---|---|
| **CRÍTICO** | ACK mesmo quando HTTP falha | corrigir antes de qualquer expansão |
| **CRÍTICO** | sem retry/DLQ | implementar semântica at-least-once |
| **CRÍTICO** | dual write Order + RabbitMQ | Transactional Outbox |
| **CRÍTICO** | E2E não prova ciclo completo | criar happy path real no CI |
| **ALTO VALOR** | persistência em memória | PostgreSQL + migrações |
| **ALTO VALOR** | evento sem envelope/versionamento | contrato `order.created.v1` |
| **ALTO VALOR** | fila conflada com nome do evento | fila específica por consumidor |
| **ALTO VALOR** | domínio permissivo | enum + invariantes + erros explícitos |
| **ALTO VALOR** | estado assíncrono não consultável | `GET /orders/{id}` |
| **ALTO VALOR** | CI não determinístico/E2E ausente | pipeline em 4 gates |
| **ALTO VALOR** | documentação contraditória | consolidar fontes canônicas |
| **MELHORIA** | logs sem correlação | JSON + IDs + duração |
| **MELHORIA** | ausência de métricas | Micrometer/Prometheus e worker metrics |
| **MELHORIA** | ausência de tracing | OpenTelemetry após estabilização |
| **MELHORIA** | containers básicos | usuário não-root e hardening |
| **NÃO AGORA** | `catalog-service` | não aumenta a tese central |
| **NÃO AGORA** | `auth-service` separado | complexidade sem ganho proporcional |
| **NÃO AGORA** | `payment-service` | fora do fluxo que queremos aprofundar |
| **NÃO AGORA** | frontend | não fortalece confiabilidade distribuída |
| **NÃO AGORA** | Kubernetes | overhead de plataforma prematuro |
| **NÃO AGORA** | LLM/AI Advisor | não existe feature LLM real para justificar runtime |
| **NÃO AGORA** | múltiplas clouds | dispersa o projeto |

---

## 5. Arquitetura-alvo

```mermaid
flowchart LR
    C[Client] -->|POST /orders| API[order-service\nJava + Spring Boot]

    API -->|same DB transaction| DB[(PostgreSQL)]
    DB --- O[(outbox_events)]

    P[Outbox Publisher] -->|read pending| O
    P -->|order.created.v1| EX{{RabbitMQ\norders.events}}
    P -->|mark published| O

    EX -->|order.created.v1| Q[scheduler.order-created.v1]
    Q --> W[scheduler-agent\nPython]
    W -->|POST /orders/{id}/confirm| API

    W -->|transient failure| R[scheduler.order-created.retry.v1]
    R -->|delay/TTL| Q
    W -->|invalid or exhausted| D[scheduler.order-created.dlq.v1]

    C -->|GET /orders/{id}| API
```

### Propriedade principal

```text
DB transaction commits:
  Order(PENDING)
  + OutboxEvent(order.created.v1)

          ↓

publisher pode tentar mais de uma vez
          ↓

RabbitMQ entrega pelo menos uma vez
          ↓

scheduler pode receber duplicado
          ↓

confirmar Order é idempotente
          ↓

Order(CONFIRMED)
```

O objetivo não é prometer “exactly once”. O projeto deve demonstrar como **at-least-once + idempotência** produz um efeito de negócio seguro.

---

## 6. Estrutura técnica-alvo

```text
microservice-shop/
├── services/
│   ├── api/
│   │   └── order-service/
│   │       ├── domain/
│   │       ├── application/
│   │       │   └── ports/
│   │       ├── infrastructure/
│   │       │   ├── persistence/
│   │       │   ├── messaging/
│   │       │   └── outbox/
│   │       └── interfaces/
│   └── workers/
│       └── scheduler-agent/
│           ├── app/
│           └── tests/
├── contracts/
│   ├── asyncapi.yaml
│   └── events/
│       └── order-created-v1.schema.json
├── tests/
│   └── bdd/
├── ml/
│   └── experiments/
├── docs/
│   ├── 00-produto/
│   ├── 02-arquitetura/
│   ├── 04-operacao/
│   └── 05-evolucao/
├── docker-compose.yml
└── Makefile
```

### Observação sobre Clean Architecture

O contrato `OrderRepository` não deve ficar conceitualmente em `infrastructure` se os casos de uso dependem dele. Ele deve ser um **port de saída da aplicação/domínio**, com implementações em infraestrutura.

Exemplo:

```text
application/ports/OrderRepository
                  ▲
                  │ implements
infrastructure/persistence/JpaOrderRepositoryAdapter
```

---

## 7. Persistência e Outbox

### Banco

**PostgreSQL** passa a ser a persistência principal do fluxo local de referência.

Tabelas mínimas:

```text
orders
- id UUID PK
- product_id VARCHAR
- quantity INTEGER
- status VARCHAR
- created_at TIMESTAMP
- updated_at TIMESTAMP

outbox_events
- id UUID PK
- aggregate_type VARCHAR
- aggregate_id UUID
- event_type VARCHAR
- event_version INTEGER
- correlation_id UUID
- payload JSONB
- occurred_at TIMESTAMP
- published_at TIMESTAMP NULL
- attempts INTEGER
```

Migrações devem ser versionadas (por exemplo, Flyway).

### Publisher

Primeira versão deve ser simples e auditável:

1. buscar pequeno lote de eventos não publicados;
2. publicar com mensagem persistente;
3. somente depois marcar `published_at`;
4. em falha, manter pendente e incrementar tentativa/log;
5. limitar concorrência para evitar complexidade precoce.

Duplicidade é possível entre passos 2 e 3. Isso é esperado e deve ser absorvido pelo desenho at-least-once.

---

## 8. Política do scheduler-agent

### Sucesso

- HTTP 2xx na confirmação;
- ACK da mensagem;
- log de sucesso com duração e IDs.

### Falha transitória

Exemplos:

- timeout;
- connection error;
- HTTP 5xx.

Ação:

- NACK sem requeue imediato;
- mensagem entra na retry queue;
- retorna para a fila principal após atraso;
- tentativas limitadas.

### Falha não recuperável

Exemplos:

- JSON inválido;
- contrato incompatível;
- campos obrigatórios ausentes;
- HTTP 4xx que represente erro definitivo.

Ação:

- publicar/rotear para DLQ;
- ACK da mensagem original somente depois da transferência segura;
- registrar motivo estruturado.

### Retries esgotados

- encaminhar para DLQ;
- preservar `eventId`, `correlationId`, payload original e motivo;
- nenhuma mensagem deve desaparecer silenciosamente.

---

## 9. Estratégia de testes

### Unitários — obrigatórios

**Java**

- criação válida;
- quantidade inválida;
- productId vazio;
- confirmação PENDING -> CONFIRMED;
- confirmação repetida;
- pedido inexistente.

**Python**

- 2xx -> ACK;
- timeout -> retry;
- 5xx -> retry;
- 4xx definitivo -> DLQ;
- JSON inválido -> DLQ;
- sem orderId -> DLQ;
- limite de retry -> DLQ;
- preservação de IDs/metadados.

### Integração

- repositório PostgreSQL real via container;
- transação salva Order + OutboxEvent;
- publisher entrega evento real ao RabbitMQ;
- falha de publicação deixa outbox pendente;
- retry do worker com servidor HTTP stub determinístico;
- DLQ contém mensagem depois de política esgotada.

### E2E / BDD

Cenário canônico:

```text
Given a stack limpa está saudável
When POST /orders cria um pedido válido
Then a API retorna 201
And GET /orders/{id} mostra PENDING ou CONFIRMED
And o evento order.created.v1 é publicado
And o scheduler processa o evento
And eventualmente GET /orders/{id} retorna CONFIRMED
```

Cenário de auditoria de evento deve usar fila exclusiva do teste:

```text
audit.test.order-created.<run-id>
```

Nunca purgar/consumir a fila operacional do scheduler para observar o evento.

---

## 10. CI alvo

```text
                 ┌──────── quality ────────┐
                 │                         │
pull request ────┼──────── unit-tests ─────┼──► integration-e2e
                 │                         │
                 └──────── security ───────┘
```

### quality

- Java format/lint;
- Python format/lint;
- TypeScript typecheck;
- validação de contratos.

### unit-tests

- Maven tests;
- Pytest;
- cobertura reportada;
- threshold só deve ser ativado depois de uma baseline real, não por número arbitrário.

### security

- dependências Python;
- dependências Node, incluindo devDependencies usadas no CI;
- dependências Java;
- secrets não devem existir no repositório.

### integration-e2e

- `docker compose up --build`;
- aguardar readiness;
- executar Cucumber;
- em falha, armazenar `docker compose ps` e logs como artifact;
- `docker compose down -v` sempre executado.

---

## 11. Documentação e apresentação de portfólio

A documentação atual deve ser consolidada. O critério é simples: **documento público precisa descrever o que existe ou uma decisão explicitamente futura**.

### README novo

Deve priorizar:

1. problema técnico;
2. arquitetura;
3. garantias de confiabilidade;
4. fluxo ponta a ponta;
5. stack;
6. como executar;
7. como validar;
8. trade-offs;
9. roadmap curto.

Evitar expressões como:

- “projeto para demonstrar”;
- “laboratório” como identidade principal;
- “pronto para demo”;
- “entregas do estágio”;
- LLM como feature quando não há feature LLM implementada.

### Documentos históricos

Arquivos históricos/contraditórios devem ser movidos para `docs/archive/` ou removidos da navegação principal. Candidatos:

- `docs/entregas-estagio.md`;
- `docs/restructure-plan.md`;
- versões antigas de “próximos passos”;
- checklists já concluídos que não ajudam a compreender o sistema atual.

Git preserva o histórico; não é necessário manter narrativa antiga na superfície principal do portfólio.

### Roadmap

Substituir o roadmap datado de 2024 por milestones de capacidade:

```text
NOW   -> reliability foundation
NEXT  -> integration + observability
LATER -> cloud or analytics differentiation
```

Nada de prometer cinco serviços futuros sem uma necessidade concreta.

---

## 12. IA/ML — decisão de escopo

### O que existe de verdade

Há dois experimentos clássicos executáveis:

- previsão de demanda com regressão;
- detecção de anomalias com Isolation Forest.

Esses artefatos são legítimos e devem continuar como `ml/experiments`.

### O que não existe de verdade

A área `ml/llm/notebooks` não possui implementação de LLM. Dependências e documentação prospectiva não devem ser apresentadas como feature funcional.

### Decisão

- remover LLM da narrativa principal;
- manter ML clássico como trilha experimental separada do caminho crítico;
- depois da plataforma distribuída estabilizada, escolher **no máximo uma** diferenciação de analytics para integrar;
- a melhor integração deve nascer de um contrato de evento/dados real, e não de uma necessidade de colocar “IA” no README.

Se o experimento de detecção de anomalias for promovido no futuro, antes deve ganhar conjunto de validação apropriado, métricas reproduzíveis e contrato claro de entrada/saída.

---

## 13. Infraestrutura/cloud — decisão de escopo

O diretório Terraform atual é apenas um placeholder e não deve ser vendido como IaC implementada.

Cloud entra apenas depois de a execução local estar determinística.

Quando chegar essa etapa:

- escolher **um** alvo de cloud;
- provisionar os recursos realmente usados pelo sistema;
- documentar custo/limites;
- manter ambiente local gratuito/reproduzível;
- não manter múltiplos provedores apenas para aumentar a lista de tecnologias.

---

## 14. Ondas de implementação

### Onda 0 — limpar a verdade do projeto

**Objetivo:** alinhar narrativa, contratos e desenho antes das mudanças estruturais.

Entregas:

- README reposicionado;
- roadmap atual;
- arquitetura canônica;
- contrato `order.created.v1`;
- ADR de Transactional Outbox;
- ADR de at-least-once + retry/DLQ;
- documentação histórica fora da navegação principal.

**Gate:** nenhuma afirmação pública importante sem evidência no código ou marcada explicitamente como futura.

---

### Onda 1 — consistência e confiabilidade

**Objetivo:** tornar o fluxo distribuído correto.

Entregas:

- domínio com invariantes;
- PostgreSQL;
- migrações;
- port de repositório corretamente posicionado;
- Transactional Outbox;
- outbox publisher;
- evento versionado;
- `GET /orders/{id}`;
- confirmação idempotente;
- scheduler com timeout;
- ACK somente após sucesso;
- retry com atraso;
- DLQ;
- topologia separada por consumidor.

**Gate:** falha de rede não pode causar perda silenciosa de mensagem.

---

### Onda 2 — provas automatizadas e segurança

**Objetivo:** provar as propriedades da Onda 1.

Entregas:

- unit tests completos;
- integração PostgreSQL/RabbitMQ;
- BDD ponta a ponta;
- cenário determinístico de retry;
- cenário DLQ;
- pipeline em quatro gates;
- instalações determinísticas;
- auditoria de dependências;
- artifacts de diagnóstico do E2E.

**Gate:** uma PR só fica verde se o pedido atravessar o sistema distribuído completo.

---

### Onda 3 — observabilidade

**Objetivo:** tornar falhas e performance diagnosticáveis.

Entregas:

- logs JSON estruturados;
- correlação ponta a ponta;
- métricas de API/outbox/worker/filas;
- tracing OpenTelemetry;
- profile opcional de observabilidade;
- runbook de retry/DLQ/outbox.

**Gate:** deve ser possível responder “onde e por que este pedido parou?” sem editar código.

---

### Onda 4 — diferenciação de portfólio

Escolher **uma frente por vez**:

**Opção A — cloud/IaC real**

- deploy de uma stack mínima;
- IaC real;
- pipeline de publish/deploy/smoke;
- custos e rollback documentados.

**Opção B — analytics/ML integrado**

- amadurecer um experimento existente;
- consumir contrato/evento real;
- avaliação reproduzível;
- serviço/worker somente se houver responsabilidade clara.

Não iniciar A e B simultaneamente antes de as ondas anteriores estarem estáveis.

---

## 15. O que reaproveitar da PR exploratória #13

A PR #13 é laboratório de descoberta, não base de merge.

| Item explorado | Decisão | Motivo |
|---|---|---|
| timeout HTTP no worker | **REAPROVEITAR CONCEITO** | requisito de confiabilidade |
| ACK somente após sucesso | **REAPROVEITAR CONCEITO** | corrige perda de mensagens |
| retry + DLQ | **REAPROVEITAR CONCEITO, REDESENHAR TOPOLOGIA** | filas devem pertencer ao consumidor |
| logs JSON | **REAPROVEITAR CONCEITO** | base de observabilidade |
| `eventId`/timestamp/correlation | **REAPROVEITAR CONCEITO** | contrato distribuído |
| confirmação repetida segura | **REAPROVEITAR CONCEITO** | necessária para at-least-once |
| CI em 4 gates | **REAPROVEITAR E REFAZER LIMPO** | boa estrutura, branch ficou iterativa |
| artifacts com logs do Compose | **REAPROVEITAR** | diagnóstico de E2E |
| declaração da mesma topologia em Java e Python | **DESCARTAR** | risco de conflito/drift |
| apenas melhorar publisher direto após `save` | **DESCARTAR** | não resolve dual write |
| header `Idempotency-Key` sem armazenamento/semântica real | **DESCARTAR POR ENQUANTO** | não prometer idempotência que não é aplicada |
| merge integral dos 42 commits | **DESCARTAR** | histórico exploratório, não implementação coerente |

---

## 16. Sequência de PRs recomendada

Para manter revisão e rollback simples:

```text
PR-01 docs/contracts baseline
PR-02 domain + PostgreSQL
PR-03 transactional outbox + publisher
PR-04 scheduler delivery semantics + retry/DLQ
PR-05 integration/E2E + CI
PR-06 observability
PR-07 optional portfolio differentiator
```

Cada PR deve terminar funcional e com seus próprios testes. Evitar outra branch com dezenas de commits heterogêneos antes de validação.

---

## 17. Definition of Done da versão elevada

O projeto só será considerado finalizado para apresentação quando:

- [ ] `docker compose up --build` sobe a stack a partir de checkout limpo;
- [ ] `POST /orders` cria pedido válido;
- [ ] pedido e evento de outbox são persistidos atomicamente;
- [ ] publisher envia `order.created.v1` ao RabbitMQ;
- [ ] scheduler confirma o pedido assíncronamente;
- [ ] `GET /orders/{id}` permite observar o resultado;
- [ ] mensagem não recebe ACK quando o efeito de negócio falha;
- [ ] falha transitória percorre retry;
- [ ] falha definitiva/retries esgotados chegam à DLQ;
- [ ] reentrega não produz efeito de negócio incorreto;
- [ ] evento possui identidade, versão e correlação;
- [ ] testes unitários Java/Python passam;
- [ ] testes de integração passam com infraestrutura real/efêmera;
- [ ] BDD prova o fluxo ponta a ponta;
- [ ] CI possui gates verdes de qualidade, testes, segurança e E2E;
- [ ] falhas E2E deixam logs diagnósticos;
- [ ] README descreve apenas capacidades reais;
- [ ] documentos históricos não confundem a arquitetura atual;
- [ ] LLM não é anunciado como feature sem implementação;
- [ ] IaC não é anunciado como implementado enquanto for placeholder.

---

## 18. Critério para qualquer nova feature

Antes de adicionar um novo serviço, tecnologia ou camada, responder:

1. Qual problema real do fluxo atual ele resolve?
2. Existe uma fronteira de responsabilidade independente?
3. O ganho arquitetural é maior que o custo operacional?
4. Há teste que demonstra a necessidade e o comportamento?
5. Essa adição fortalece a tese principal do projeto?

Se as respostas não forem claras, a feature não entra.

---

## 19. Resultado esperado

Ao final, o valor do Microservice Shop não estará na quantidade de tecnologias listadas, mas na capacidade de mostrar, com código e testes, decisões que aparecem em sistemas distribuídos reais:

- transações e consistência;
- outbox;
- AMQP;
- at-least-once;
- idempotência;
- retry/DLQ;
- contratos versionados;
- observabilidade;
- testes multi-processo;
- CI reproduzível;
- trade-offs documentados.

Essa é a linha de evolução que deve orientar todas as próximas alterações.