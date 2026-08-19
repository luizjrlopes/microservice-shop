# Próximos Passos — Documento Substituído

> **Status:** histórico / não canônico.
>
> Este arquivo registrava um plano anterior de expansão do Microservice Shop. Ele foi substituído pela auditoria e pelo plano arquitetural aprovados em agosto de 2026.

## Fontes vigentes

Use estas referências para qualquer decisão atual:

1. [`05-evolucao/AUDITORIA_ESTADO_ATUAL.md`](05-evolucao/AUDITORIA_ESTADO_ATUAL.md) — diagnóstico factual da `main` antes da nova evolução;
2. [`05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md) — arquitetura-alvo, gaps e Definition of Done;
3. [`../ROADMAP.md`](../ROADMAP.md) — ordem atual das ondas de implementação;
4. [`architecture.md`](architecture.md) — arquitetura implementada no momento.

## Por que este plano foi substituído

A estratégia anterior priorizava expansão de escopo com itens como `catalog-service`, `auth-service` e `ai-advisor` antes de resolver propriedades fundamentais do fluxo já existente.

A auditoria posterior mostrou que o maior ganho técnico está em aprofundar o núcleo atual:

- consistência entre pedido e evento;
- Transactional Outbox;
- entrega at-least-once;
- idempotência;
- retry/DLQ;
- contratos versionados;
- E2E distribuído no CI;
- observabilidade correlacionada.

## Valor histórico

Este arquivo permanece versionado apenas para preservar a evolução das decisões do projeto. Ele **não deve ser usado como backlog vigente nem como descrição do estado atual**.