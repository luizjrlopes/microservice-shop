# Escopo Atual — Microservice Shop

Este documento separa **implementado**, **em evolução** e **fora do núcleo atual**. Ele não deve antecipar capacidades ainda inexistentes.

## Implementado

| Capacidade | Estado |
|---|---|
| `order-service` em Java 17 + Spring Boot | ✅ |
| `POST /orders` | ✅ |
| `POST /orders/{id}/confirm` | ✅ |
| repositório em memória para pedidos | ✅ |
| publicação de `order.created` no RabbitMQ | ✅ |
| `scheduler-agent` Python consumindo AMQP | ✅ |
| chamada assíncrona do worker para confirmação | ✅ |
| Docker Compose com RabbitMQ + serviços | ✅ |
| testes unitários Java | ✅ |
| testes unitários Python | ✅ |
| BDD em TypeScript/Cucumber | ✅ |
| GitHub Actions para lint/test/security | ✅ |
| ADRs arquiteturais | ✅ |
| previsão de demanda experimental | ✅ |
| detecção de anomalias experimental | ✅ |

## Limitações conhecidas do estado atual

- persistência é in-memory;
- salvar pedido e publicar evento são operações separadas;
- o worker confirma mensagens mesmo quando a chamada HTTP pode falhar;
- não há política completa de retry/DLQ;
- o contrato do evento não possui envelope/versionamento suficiente;
- não existe `GET /orders/{id}`;
- a suíte BDD atual não prova o ciclo assíncrono completo de forma isolada;
- o CI não sobe a stack distribuída como gate obrigatório;
- observabilidade se limita a health/logs básicos;
- a área LLM ainda não possui feature implementada;
- Terraform existente é apenas espaço/documentação, não infraestrutura executável.

Esses pontos são detalhados em [`../05-evolucao/AUDITORIA_ESTADO_ATUAL.md`](../05-evolucao/AUDITORIA_ESTADO_ATUAL.md).

## Em evolução aprovada

A próxima evolução do núcleo inclui:

- PostgreSQL;
- migrações versionadas;
- domínio com invariantes e `OrderStatus`;
- `GET /orders/{id}`;
- Transactional Outbox;
- evento `order.created.v1` versionado;
- filas específicas por consumidor;
- timeout, ACK correto, retry e DLQ;
- confirmação idempotente;
- integração/E2E no CI;
- logs correlacionados e, posteriormente, métricas/tracing.

A ordem está em [`../../ROADMAP.md`](../../ROADMAP.md).

## Fora do núcleo atual

Não fazem parte do escopo imediato:

- frontend;
- `catalog-service`;
- `auth-service` dedicado;
- `payment-service`;
- Kubernetes;
- múltiplas clouds;
- `AI Advisor`/serviço LLM.

Esses itens só devem voltar ao planejamento se houver uma necessidade concreta que justifique uma nova fronteira ou uma nova capacidade de produto.

## Critério de entrada no escopo

Uma nova feature ou serviço deve responder positivamente a pelo menos uma destas perguntas:

1. resolve um problema real do fluxo de pedidos?
2. aumenta uma propriedade arquitetural que podemos testar?
3. permite demonstrar uma decisão de engenharia com trade-offs claros?
4. pode ser executado e validado de forma reproduzível?

Se a única justificativa for “adicionar mais uma tecnologia ao projeto”, o item permanece fora do núcleo.