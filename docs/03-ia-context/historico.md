# Histórico de Decisões — microservice-shop

### 2026-06-22 — Fase 0 — RabbitMQ para mensageria assíncrona
**Contexto:** Comunicação assíncrona entre order-service e scheduler  
**Decisão:** RabbitMQ com exchange direct + routing key `order.created`  
**Por quê:** Desacoplamento real; simples para demonstração; management UI para debug  
**Onde também registrar:** ADR-001

---

### 2026-06-22 — Fase 0 — InMemoryOrderRepository no MVP
**Contexto:** Persistência necessária mas banco real adicionaria complexidade sem valor imediato  
**Decisão:** Interface `OrderRepository` implementada in-memory; banco real como próximo passo  
**Por quê:** Clean Architecture permite trocar a implementação sem alterar use cases  
**Onde também registrar:** ADR-002

---

### 2026-06-22 — Fase 0 — Separação experiments ML do código de produção
**Contexto:** Experimentos de IA/LLM não devem contaminar os serviços de produção  
**Decisão:** Todo código ML/LLM vive em `ml/` — serviços em `services/` são "tradicionais"  
**Por quê:** Ciclos de vida diferentes; experimentos podem quebrar sem afetar a demo principal
