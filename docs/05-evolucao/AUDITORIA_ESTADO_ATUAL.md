# Auditoria do Estado Atual — Microservice Shop

> **Snapshot auditado:** `main` em `baf92ca59c162df80840e0af41c723a9f13fb16f`  
> **Natureza:** diagnóstico factual do repositório antes da nova evolução  
> **Regra:** este documento descreve o estado encontrado; decisões de evolução estão em `PLANO_EVOLUCAO_ARQUITETURAL.md`.

---

## 1. Escopo auditado

A auditoria cobriu:

- apresentação pública e README;
- documentação de produto e arquitetura;
- `order-service` Java/Spring Boot;
- `scheduler-agent` Python;
- RabbitMQ e Docker Compose;
- testes Java, Python e BDD TypeScript;
- Makefile e GitHub Actions;
- experimentos ML/LLM;
- infraestrutura como código;
- PR exploratória de hardening #13.

---

## 2. Resumo executivo

O projeto possui uma **base técnica melhor do que a apresentação atual sugere**: Java, Python, RabbitMQ, Docker Compose, testes unitários, BDD, ADRs e experimentos de ML executáveis já existem.

O principal problema não é falta de tecnologia. É a combinação de:

1. **falhas reais de confiabilidade no fluxo assíncrono**;
2. **consistência não garantida entre persistência e publicação de evento**;
3. **testes e CI que não provam o fluxo distribuído completo**;
4. **documentação pública contraditória ou desatualizada**;
5. **promessas de IA/LLM e IaC maiores que a implementação real**;
6. **roadmap inflado com serviços que não fortalecem o problema central**.

A melhor evolução é aprofundar o fluxo atual antes de aumentar o número de serviços.

---

## 3. Scorecard heurístico

Escala apenas para priorização interna:

| Área | Estado | Leitura |
|---|---:|---|
| Fundamentos de arquitetura | 3/5 | boa separação inicial, mas fronteiras ainda inconsistentes |
| Confiabilidade distribuída | 1/5 | ACK inseguro, sem retry/DLQ, sem outbox |
| Domínio/API | 2/5 | fluxo mínimo existe; invariantes e consulta de estado faltam |
| Mensageria/contratos | 2/5 | RabbitMQ funciona, contrato é pouco versionado/rastreável |
| Testes unitários | 3/5 | Java e Python têm testes reais |
| Integração/E2E | 1.5/5 | BDD existe, mas compete com consumidor e não roda como gate real |
| CI | 2/5 | lint/test/security existem, porém pipeline não valida o sistema completo |
| Segurança de dependências | 1.5/5 | auditoria Node está configurada de forma enganosa |
| Observabilidade | 1/5 | healthcheck básico; sem correlação, métricas ou tracing |
| Documentação técnica | 3/5 | há bastante material, mas duplicado/contraditório |
| Apresentação de portfólio | 2/5 | narrativa de laboratório/estágio reduz a força do código |
| ML clássico | 3/5 | há scripts reais de treino/inferência |
| LLM | 0.5/5 | dependências e documentação prospectiva; nenhum notebook implementado |
| IaC | 0.5/5 | apenas placeholder Terraform |

---

## 4. Achados críticos

### A-01 — perda potencial de mensagem no worker

**Arquivo:** `services/workers/scheduler-agent/app.py`

O callback atual executa:

```python
try:
    requests.post(...)
finally:
    channel.basic_ack(...)
```

Consequências:

- timeout/erro de conexão pode resultar em ACK;
- HTTP 5xx também não impede ACK;
- nenhuma política de reprocessamento é aplicada;
- falha pode desaparecer da fila sem concluir o efeito de negócio.

**Severidade:** CRÍTICA.

---

### A-02 — chamada HTTP sem timeout

**Arquivo:** `services/workers/scheduler-agent/app.py`

`requests.post()` não define `timeout`.

**Risco:** thread/processo pode aguardar indefinidamente dependendo da falha de rede.

**Severidade:** ALTA.

---

### A-03 — ausência de retry e Dead Letter Queue

**Arquivos:**

- `services/api/order-service/.../MessagingConfig.java`
- `services/workers/scheduler-agent/app.py`

A topologia atual possui apenas:

- `order.exchange`;
- routing key `order.created`;
- fila `order.created`.

Não há tratamento separado de falha transitória ou definitiva.

**Severidade:** CRÍTICA.

---

### A-04 — dual write na criação de pedido

**Arquivos:**

- `CreateOrderService.java`;
- `OrderController.java`;
- `OrderEventPublisher.java`.

Fluxo atual:

```text
CreateOrderService.save(order)
        ↓
OrderController.eventPublisher.publish(order)
```

Persistência lógica e publicação AMQP são duas operações independentes.

Mesmo com banco real, uma falha entre as operações gera estados como:

```text
Order persistido = SIM
order.created publicado = NÃO
```

**Severidade:** CRÍTICA.

---

## 5. Achados de domínio e API

### A-05 — status como string livre

`Order` mantém status em `String` e expõe `setStatus(String)`.

Isso permite estados inválidos e não expressa transições de domínio.

**Severidade:** MÉDIA/ALTA.

### A-06 — ausência de invariantes de entrada

O código de criação não valida explicitamente:

- `productId` vazio/nulo;
- quantidade zero/negativa.

**Severidade:** ALTA para coerência do contrato.

### A-07 — não existe GET de pedido

A API cria e confirma pedidos, mas não possui `GET /orders/{id}`.

Isso dificulta observar o resultado assíncrono sem acessar internals/logs.

**Severidade:** ALTO VALOR DE MELHORIA.

### A-08 — exceção genérica para pedido não encontrado

`ConfirmOrderService` lança `IllegalArgumentException("Order not found")`.

O contrato HTTP não possui uma modelagem explícita do erro de domínio.

**Severidade:** MÉDIA.

---

## 6. Achados de arquitetura

### A-09 — port de repositório localizado em infraestrutura

`CreateOrderService` e `ConfirmOrderService` dependem de `com.shop.order.infrastructure.OrderRepository`.

Apesar de a documentação chamar o desenho de Clean Architecture, o port utilizado pela aplicação está dentro da camada de infraestrutura.

**Leitura:** a intenção arquitetural é boa, mas a direção das dependências ainda precisa ser refinada.

**Severidade:** MÉDIA / ALTO VALOR DE CLAREZA.

### A-10 — persistência apenas em memória

`InMemoryOrderRepository` usa `ConcurrentHashMap`.

Reiniciar o serviço elimina os pedidos e impede provar consistência transacional real.

**Severidade:** ALTA para a tese arquitetural desejada.

---

## 7. Achados de contratos/mensageria

### A-11 — evento sem identidade e versionamento

`OrderCreatedEvent` contém:

```text
id
productId
quantity
status
```

Faltam metadados essenciais para rastreamento/evolução:

- `eventId`;
- `eventVersion`;
- `occurredAt`;
- `correlationId`.

**Severidade:** ALTA.

### A-12 — nome de evento e fila conflados

A fila operacional chama-se `order.created`, igual ao conceito de evento/routing key.

Isso cria acoplamento entre produtor e consumidor e torna difícil adicionar consumidores independentes.

**Severidade:** MÉDIA/ALTA.

---

## 8. Achados de testes

### A-13 — README afirma que não há testes Java, mas há

O README diz que `make api-test` executa Maven “ainda sem casos”.

Entretanto existem testes como:

- `CreateOrderServiceTest`;
- `ConfirmOrderServiceTest`;
- `OrderControllerTest`.

**Impacto:** repositório parece menos maduro do que realmente é e a documentação perde credibilidade.

### A-14 — BDD promete comportamento inexistente

`order_event.feature` espera:

- produto `unknown` -> `404`;
- quantidade `0` -> `400`.

O código atual não implementa catálogo nem validação correspondente.

**Impacto:** especificação e implementação divergem.

### A-15 — BDD compete com o scheduler

Os steps:

- purgam `order.created`;
- consomem a própria fila `order.created` para verificar evento.

Quando scheduler e BDD estão ativos, ambos disputam mensagens da mesma fila.

**Impacto:** teste flakey e semântica errada de pub/sub.

### A-16 — BDD não prova confirmação assíncrona

O teste observa publicação do evento, mas não fecha o ciclo:

```text
POST /orders
-> RabbitMQ
-> scheduler-agent
-> POST /confirm
-> estado final CONFIRMED
```

**Severidade:** CRÍTICA para a proposta de sistema distribuído.

### A-17 — testes Python codificam comportamento inseguro

O teste atual espera ACK após o callback e também espera ACK quando `orderId` está ausente.

Não há testes de timeout, 5xx, retry ou DLQ.

**Severidade:** ALTA.

---

## 9. Achados de CI/DevEx

### A-18 — `npm install` no CI apesar de lockfile

O workflow usa `npm install` em `tests/bdd`, embora exista `package-lock.json`.

**Impacto:** resolução de dependências menos determinística.

### A-19 — BDD não faz parte do gate principal

`make test` executa apenas:

- Maven tests;
- Pytest.

O workflow não sobe Docker Compose nem roda o Cucumber contra a stack real.

**Impacto:** CI pode ficar verde sem validar integração Java + RabbitMQ + Python.

### A-20 — auditoria npm ignora exatamente as dependências existentes

`tests/bdd/package.json` declara as dependências em `devDependencies`.

O Makefile executa:

```bash
npm audit --omit=dev
```

Portanto o gate de segurança exclui o conjunto de dependências usado pelo BDD/CI.

**Impacto:** falsa sensação de auditoria.

**Severidade:** ALTA.

---

## 10. Achados de documentação e portfólio

### A-21 — README enquadra o sistema como laboratório/demonstração

A abertura enfatiza que o projeto foi pensado para “demonstrar padrões” e menciona ferramentas prontas para “laboratório”.

**Impacto:** reduz percepção de engenharia de produto/sistema.

### A-22 — documento de visão define o usuário como “desenvolvedor que quer demonstrar arquitetura”

`docs/00-produto/visao-e-objetivo.md` define o problema em termos de portfólio e afirma que o objetivo principal é demonstrar tecnologias.

**Impacto:** o projeto não possui uma tese técnica/produto própria; parece existir somente para currículo.

### A-23 — roadmap parado em 2024

`ROADMAP.md` ainda está dividido em Q2/Q3/Q4 2024.

Além de desatualizado, promete:

- catalog-service;
- auth-service;
- AI Advisor;
- playbooks por LLM;
- várias expansões antes de o core estar confiável.

**Impacto:** dívida narrativa e scope inflation.

### A-24 — documentação histórica contradiz o código atual

`docs/restructure-plan.md` afirma que:

- Maven não possui testes;
- worker não possui testes;
- BDD possui problemas de dependências historicamente já alterados.

Essas afirmações não representam mais a árvore atual.

### A-25 — arquivo público centrado em “entregas do estágio”

`docs/entregas-estagio.md` organiza o projeto pelos requisitos de um estágio e por pilares LLM/MLOps/documentação.

**Impacto:** contexto histórico domina uma superfície que deveria explicar o sistema atual.

### A-26 — arquitetura duplicada

Existem múltiplas fontes descrevendo arquitetura e próximos passos, incluindo:

- `docs/architecture.md`;
- `docs/02-arquitetura/visao-tecnica.md`;
- `docs/restructure-plan.md`;
- `docs/proximos-passos.md`;
- `ROADMAP.md`.

**Impacto:** não há fonte canônica evidente e documentos divergem com o tempo.

---

## 11. Achados de IA/ML

### A-27 — ML clássico é real

Existem scripts executáveis para:

- previsão de demanda usando regressão;
- detecção de anomalias usando Isolation Forest;
- treino/inferência e datasets de exemplo.

**Conclusão:** essa parte não deve ser descartada como “fake AI”. É uma trilha experimental legítima.

### A-28 — LLM não está implementado

`ml/llm/notebooks` contém apenas `.gitkeep`.

Existem dependências como `openai` e `tiktoken` e documentação sobre futuros experimentos, porém nenhum notebook/feature LLM implementado.

**Impacto:** README/documentos dão destaque maior a LLM do que o código sustenta.

### A-29 — metodologia de anomaly detection ainda é experimental

O treino de Isolation Forest calcula precisão sobre o próprio dataset usado para `fit`.

**Impacto:** suficiente como baseline exploratório, não suficiente para promover o modelo como capacidade operacional validada.

---

## 12. Achados de infraestrutura

### A-30 — Terraform é placeholder

`infra/terraform` contém apenas README com convenções futuras e nenhum recurso Terraform.

**Conclusão:** IaC não deve ser apresentada como implementada.

### A-31 — Docker funciona como base, mas não está endurecido

Pontos de evolução:

- credenciais RabbitMQ locais explícitas em vez de depender de defaults;
- usuários não-root nos containers;
- readiness/health coerentes;
- versões/imagens controladas;
- diagnóstico melhor de falhas da stack.

---

## 13. O que a PR exploratória #13 provou

A PR #13 foi útil como experimento e revelou que as melhorias mais valiosas são de confiabilidade, não de quantidade de serviços.

Conceitos tecnicamente úteis explorados nela:

- timeout HTTP;
- ACK após sucesso;
- retry/DLQ;
- logs JSON;
- identidade/correlação de evento;
- confirmação repetida segura;
- CI E2E com artifacts diagnósticos.

Entretanto, a branch acumulou dezenas de commits e correções incrementais, incluindo problemas de topologia/configuração da stack. Portanto ela deve permanecer **congelada e não mergeada**.

A implementação final deve nascer de `main` e importar somente conceitos que sobrevivam ao plano arquitetural.

---

## 14. Causas-raiz

Os achados podem ser reduzidos a cinco causas principais:

### C1 — foco histórico em variedade de tecnologias

O projeto foi narrado como demonstração de Java + Python + RabbitMQ + IA/ML, incentivando expansão horizontal antes do hardening do fluxo central.

### C2 — ausência de contrato explícito de confiabilidade

Não havia decisão clara sobre:

- quando ACK ocorre;
- quando retry ocorre;
- o que vai para DLQ;
- como duplicatas são tratadas;
- como persistência e evento permanecem consistentes.

### C3 — BDD criado como verificação de mensagem, não de comportamento do sistema

A suíte observa a fila diretamente e acaba competindo com o consumidor que deveria validar.

### C4 — documentação acumulativa

Novos documentos foram adicionados sem substituir/arquivar os anteriores, gerando múltiplas versões da verdade.

### C5 — roadmap orientado a features, não a propriedades arquiteturais

O roadmap adicionava serviços e IA antes de resolver entrega, consistência, observabilidade e teste distribuído.

---

## 15. Conclusão da auditoria

A base atual **vale ser evoluída**, não substituída.

O diferencial potencial está no fluxo pequeno:

```text
cliente
  -> order-service
  -> persistência
  -> RabbitMQ
  -> scheduler-agent
  -> order-service
```

Se esse fluxo provar:

- consistência transacional;
- at-least-once;
- idempotência;
- retry/DLQ;
- contratos versionados;
- observabilidade;
- testes distribuídos reais;

então o repositório passa a demonstrar engenharia mais madura do que um diagrama com muitos microsserviços superficiais.

As ações propostas a partir deste diagnóstico estão em:

`docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`.