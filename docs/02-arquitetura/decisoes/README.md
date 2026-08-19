# Decisões de Arquitetura (ADRs)

| ADR | Título | Status |
|---|---|---|
| ADR-000 | Template | Referência |
| ADR-001 | RabbitMQ como mensageria | Aceita |
| ADR-002 | InMemoryOrderRepository no estágio inicial | Substituída pelo ADR-005 |
| ADR-003 | Transactional Outbox para publicação de eventos | Aceita para implementação |
| ADR-004 | At-least-once, retry com atraso e Dead Letter Queue | Aceita para implementação |
| ADR-005 | PostgreSQL como persistência do `order-service` | Aceita |

## Leitura de status

- **Aceita:** decisão refletida no estado atual ou mantida como fundamento.
- **Substituída:** decisão histórica preservada, mas não vigente.
- **Aceita para implementação:** arquitetura aprovada, porém ainda não deve ser descrita como runtime concluído.

A sequência de implementação está em [`../../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](../../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).
