# Roadmap — Microservice Shop

Este roadmap descreve a evolução do projeto a partir do **estado implementado**. Itens concluídos correspondem a código, testes, contratos ou automação existentes no repositório; itens futuros permanecem explícitos para evitar confundir arquitetura-alvo com capacidade entregue.

## Onda 0 — Verdade do projeto

- [x] auditar o estado atual;
- [x] definir arquitetura-alvo;
- [x] reposicionar README e visão de produto;
- [x] separar documentação vigente de documentos históricos;
- [x] definir `order.created.v1` em JSON Schema + AsyncAPI;
- [x] registrar ADR de Transactional Outbox;
- [x] registrar ADR de at-least-once + retry/DLQ.

**Gate:** concluído.

## Onda 1 — Confiabilidade e consistência

### Fundação de domínio e persistência

- [x] introduzir PostgreSQL como persistência do `order-service`;
- [x] versionar migrações com Flyway;
- [x] modelar `OrderStatus` e invariantes de domínio;
- [x] mover o port de repositório para a camada de aplicação;
- [x] adicionar adapter PostgreSQL;
- [x] adicionar `GET /orders/{id}`;
- [x] tornar confirmação repetida segura no domínio;
- [x] validar persistência com PostgreSQL real via Testcontainers.

### Consistência estado + evento

- [x] implementar Transactional Outbox;
- [x] criar migration `outbox_events`;
- [x] persistir pedido + evento na mesma transação;
- [x] criar publisher agendado de eventos pendentes;
- [x] aguardar publisher confirm do RabbitMQ antes de marcar evento como publicado;
- [x] adotar `order.created.v1` no runtime;
- [x] remover dual write direto após `save`;
- [x] testar que falha do publisher mantém o evento pendente e registra a falha.

### Semântica de entrega

- [x] usar fila exclusiva do `scheduler-agent`;
- [x] usar ACK manual e somente concluir a entrega após efeito bem-sucedido ou encaminhamento seguro;
- [x] aplicar timeout na chamada HTTP de confirmação;
- [x] implementar retry com atraso via fila TTL;
- [x] implementar Dead Letter Queue;
- [x] preservar `eventId`, `correlationId` e contagem de retry;
- [x] reencaminhar a entrega original quando a publicação de retry/DLQ falha;
- [x] manter confirmação de pedido idempotente.

**Gate da Onda 1:** concluído no desenho atual — falha de publicação não apaga o registro do Outbox e falha transitória do consumidor possui caminho explícito de retry/DLQ.

## Onda 2 — Provas automatizadas e quality gates

- [x] cobrir políticas de ACK, retry, DLQ e falha de republicação do worker;
- [ ] adicionar teste de integração dedicado RabbitMQ + Outbox, além do BDD distribuído;
- [x] criar fila exclusiva de auditoria para BDD;
- [x] provar `POST /orders -> order.created.v1 -> worker -> CONFIRMED`;
- [x] validar envelope versionado do evento no BDD;
- [x] usar instalação determinística com `npm ci` na suíte BDD;
- [ ] ampliar auditoria de dependências para toda a stack, além dos checks atuais;
- [x] separar pipeline em quality, unit-tests, security e integration-e2e;
- [x] bloquear o E2E quando gates anteriores falham;
- [x] publicar estado e logs dos containers como artifact em falhas E2E.

**Gate:** operacional no CI; o fluxo distribuído real faz parte da aprovação da PR. Os dois itens ainda abertos são hardening adicional, não substitutos do gate existente.

## Onda 3 — Observabilidade operacional

- [x] logs JSON estruturados no `scheduler-agent`;
- [ ] padronizar logs estruturados também no `order-service`;
- [ ] comprovar `correlationId`, `eventId` e `orderId` ponta a ponta em logs consultáveis;
- [ ] adicionar métricas de domínio/Outbox ao `order-service`;
- [x] expor métricas de processamento, retry, DLQ e latência no `scheduler-agent`;
- [x] disponibilizar profile opcional de Prometheus no Compose;
- [ ] adicionar dashboards/alertas mínimos para os sinais principais;
- [ ] adicionar instrumentação OpenTelemetry;
- [ ] documentar troubleshooting orientado a sinais.

**Gate:** ainda aberto — deve ser possível localizar onde um pedido parou usando sinais operacionais, sem editar código.

## Onda 4 — Diferenciação principal

Somente após fechar o gate de observabilidade, escolher **uma** frente principal para aumentar profundidade sem inflar artificialmente o sistema.

### Opção A — Cloud/IaC executável

- IaC real para uma arquitetura coerente com o runtime;
- build/deploy automatizado;
- health/smoke checks;
- estratégia explícita de rollback;
- evidência de ambiente implantado.

### Opção B — ML integrado

- selecionar um dos experimentos existentes;
- definir contrato de entrada/saída;
- integrar de forma assíncrona, sem bloquear o core transacional;
- medir qualidade do modelo e comportamento operacional;
- manter fallback explícito quando a capacidade de ML estiver indisponível.

A decisão entre A e B deve ser registrada por ADR quando o gate da Onda 3 estiver fechado.

## Fora do roadmap imediato

- criar `catalog-service`, `auth-service`, `payment-service` ou outros serviços apenas para aumentar contagem;
- frontend sem necessidade de produto concreta;
- Kubernetes por demonstração;
- múltiplas clouds;
- `AI Advisor`/LLM sem feature integrada e mensurável.

## Referência arquitetural

A motivação e os trade-offs estão em [`docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md). O código, os testes e os contratos executáveis têm precedência para determinar o estado implementado.