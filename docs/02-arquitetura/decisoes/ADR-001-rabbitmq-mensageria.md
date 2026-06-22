# ADR-001: RabbitMQ como broker de mensageria

**Data:** 2026-06-22  
**Status:** Aceita

## Contexto

A confirmação de pedido precisa acontecer de forma assíncrona — o order-service não deve aguardar a confirmação para responder ao cliente. Precisamos de um mecanismo de comunicação assíncrona entre order-service e scheduler-agent.

## Opções consideradas

### Opção A: Polling HTTP (scheduler checa pedidos pendentes periodicamente)
- **Prós:** Simples, sem infra extra
- **Contras:** Latência, carga desnecessária no order-service, acoplamento temporal

### Opção B: RabbitMQ (broker de mensageria)
- **Prós:** Desacoplamento real, entrega garantida, padrão de mercado para microsserviços
- **Contras:** Infra adicional (RabbitMQ)

### Opção C: Kafka
- **Prós:** Alta throughput, log persistente
- **Contras:** Mais complexo para volume baixo de estudo; RabbitMQ é mais simples para começar

## Decisão

**Opção B — RabbitMQ.** Padrão adequado para o volume de demonstração, imagem oficial disponível no Docker Hub, management UI incluída para facilitar debugging durante apresentações.

## Consequências

**Positivas:** Comunicação assíncrona real; scheduler pode escalar independentemente  
**Negativas:** RabbitMQ é dependência obrigatória; precisa de healthcheck antes dos serviços
