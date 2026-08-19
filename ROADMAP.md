# Roadmap — Microservice Shop

Este roadmap descreve a sequência de evolução do projeto. Cada etapa só avança quando os critérios técnicos da etapa anterior estão satisfeitos.

## Onda 0 — Verdade do projeto

- [x] auditar o estado atual;
- [x] definir arquitetura-alvo;
- [x] reposicionar README e visão de produto;
- [x] eliminar contradições principais entre documentação e código;
- [x] marcar documentos históricos como não canônicos;
- [x] consolidar fontes de arquitetura e evolução;
- [x] definir `order.created.v1` em JSON Schema + AsyncAPI;
- [x] registrar ADR de Transactional Outbox;
- [x] registrar ADR de at-least-once + retry/DLQ.

**Gate:** concluído.

## Onda 1 — Confiabilidade e consistência

### Fundação de domínio e persistência — PR-02

- [x] introduzir PostgreSQL como persistência do `order-service`;
- [x] versionar migrações com Flyway;
- [x] modelar `OrderStatus` e invariantes de domínio;
- [x] mover o port de repositório para a camada de aplicação;
- [x] adicionar adapter PostgreSQL;
- [x] adicionar `GET /orders/{id}`;
- [x] tornar confirmação repetida segura no domínio;
- [x] validar persistência com PostgreSQL real via Testcontainers.

### Consistência estado + evento — PR-03

- [ ] implementar Transactional Outbox;
- [ ] criar publisher de eventos pendentes;
- [ ] adotar `order.created.v1` no runtime;
- [ ] remover publicação direta depois de `save`;
- [ ] provar que falha do broker deixa evento pendente.

### Semântica de entrega — PR-04

- [ ] separar filas por consumidor;
- [ ] corrigir ACK do worker;
- [ ] implementar timeout HTTP;
- [ ] implementar retry com atraso;
- [ ] implementar Dead Letter Queue;
- [ ] preservar identidade/correlação durante retries;
- [ ] provar que reentrega não produz efeito de negócio incorreto.

**Gate da Onda 1:** falha de rede não pode causar perda silenciosa de mensagem.

## Onda 2 — Provas automatizadas

- [ ] ampliar testes unitários de políticas de entrega;
- [ ] adicionar integração RabbitMQ/Outbox;
- [ ] criar fila exclusiva de auditoria para BDD;
- [ ] provar `POST /orders -> evento -> worker -> CONFIRMED`;
- [ ] usar instalações determinísticas no CI;
- [ ] executar auditoria real das dependências usadas;
- [ ] separar pipeline em quality, unit-tests, security e integration-e2e;
- [ ] publicar logs dos containers como artifact em falhas E2E.

**Gate:** uma PR não fica verde se o ciclo distribuído real estiver quebrado.

## Onda 3 — Observabilidade

- [ ] logs JSON estruturados;
- [ ] `correlationId`, `eventId` e `orderId` ponta a ponta;
- [ ] métricas de pedidos, Outbox, retries e DLQ;
- [ ] instrumentação OpenTelemetry;
- [ ] profile opcional de observabilidade no Compose;
- [ ] documentação de troubleshooting orientada a sinais.

**Gate:** deve ser possível responder onde um pedido parou sem editar código.

## Onda 4 — Diferenciação

Somente após o core distribuído estar estável, escolher **uma** frente principal:

### Opção A — Cloud/IaC real

- IaC executável para uma arquitetura coerente;
- pipeline de build/deploy;
- smoke tests e rollback.

### Opção B — ML integrado

- selecionar um experimento atual;
- definir contrato de entrada/saída;
- integrar sem bloquear o processamento principal;
- medir qualidade e comportamento operacional.

A decisão entre A e B deve ser registrada por ADR.

## Fora do roadmap imediato

- `catalog-service` apenas para aumentar o número de serviços;
- `auth-service` separado sem necessidade concreta;
- `payment-service`;
- frontend;
- Kubernetes;
- múltiplas clouds;
- `AI Advisor`/LLM sem feature real implementada.

## Referência arquitetural

A motivação e os critérios completos estão em [`docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).