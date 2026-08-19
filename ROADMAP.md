# Roadmap — Microservice Shop

Este roadmap descreve a sequência de evolução do projeto. Ele não usa datas artificiais: cada etapa só avança quando os critérios técnicos da etapa anterior estão satisfeitos.

## Onda 0 — Verdade do projeto

Objetivo: alinhar documentação, arquitetura descrita e capacidades reais.

- [x] auditar o estado atual;
- [x] definir arquitetura-alvo;
- [ ] reposicionar README e visão de produto;
- [ ] eliminar contradições entre documentação e código;
- [ ] marcar documentos históricos como não canônicos;
- [ ] consolidar fontes de arquitetura e evolução.

**Saída:** qualquer pessoa deve conseguir distinguir claramente o que está implementado, o que está planejado e o que é histórico.

## Onda 1 — Confiabilidade e consistência

Objetivo: transformar o fluxo de pedidos em um fluxo distribuído tecnicamente consistente.

- [ ] introduzir PostgreSQL como persistência do `order-service`;
- [ ] versionar migrações;
- [ ] modelar `OrderStatus` e invariantes de domínio;
- [ ] adicionar `GET /orders/{id}`;
- [ ] implementar Transactional Outbox;
- [ ] criar publisher de eventos pendentes;
- [ ] versionar `order.created.v1` com identidade e correlação;
- [ ] separar nomes de evento das filas de consumidores;
- [ ] corrigir ACK do worker;
- [ ] implementar timeout HTTP;
- [ ] implementar retry com atraso;
- [ ] implementar Dead Letter Queue;
- [ ] tornar confirmação repetida idempotente.

**Saída:** persistir pedido e registrar evento na mesma transação; processamento assíncrono tolerante a reentrega e falhas transitórias.

## Onda 2 — Provas automatizadas

Objetivo: provar as propriedades arquiteturais no CI.

- [ ] ampliar testes unitários de domínio e políticas de retry;
- [ ] adicionar testes de integração com PostgreSQL;
- [ ] adicionar testes de integração com RabbitMQ;
- [ ] validar Outbox e publicação;
- [ ] criar fila exclusiva de auditoria para BDD;
- [ ] provar `POST /orders -> evento -> worker -> CONFIRMED`;
- [ ] usar `npm ci` no CI;
- [ ] executar auditoria real das dependências usadas;
- [ ] separar pipeline em quality, unit-tests, security e integration-e2e;
- [ ] publicar logs dos containers como artifact em falhas E2E.

**Saída:** uma PR não fica verde se o ciclo distribuído real estiver quebrado.

## Onda 3 — Observabilidade

Objetivo: tornar o fluxo assíncrono rastreável sem tornar o ambiente local pesado.

- [ ] logs JSON estruturados;
- [ ] `correlationId`, `eventId` e `orderId` ponta a ponta;
- [ ] métricas de pedidos, Outbox, retries e DLQ;
- [ ] instrumentação OpenTelemetry;
- [ ] profile opcional de observabilidade no Compose;
- [ ] documentação de troubleshooting orientada a sinais.

**Saída:** um pedido pode ser rastreado entre API, banco, broker e worker.

## Onda 4 — Diferenciação

Somente após o core distribuído estar estável, escolher **uma** frente principal de diferenciação.

### Opção A — Cloud/IaC real

- IaC executável para uma arquitetura coerente;
- pipeline de build/deploy;
- smoke tests e estratégia de rollback.

### Opção B — ML integrado

- selecionar um dos experimentos atuais;
- definir contrato de entrada/saída;
- integrar ao fluxo sem bloquear o processamento principal;
- medir qualidade e comportamento operacional.

A decisão entre A e B deve ser registrada por ADR. Não implementar as duas frentes simultaneamente sem justificativa.

## Fora do roadmap imediato

Não fazem parte das ondas atuais:

- `catalog-service` apenas para aumentar o número de serviços;
- `auth-service` separado sem necessidade de produto;
- `payment-service`;
- frontend;
- Kubernetes;
- múltiplas clouds;
- `AI Advisor`/LLM sem feature real implementada.

## Referência arquitetural

A motivação e os critérios completos estão em [`docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).