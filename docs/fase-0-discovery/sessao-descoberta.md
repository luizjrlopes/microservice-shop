# Sessão de Discovery — microservice-shop

**Data:** 2026-06-22 (reconstituída a partir do código)  
**Status:** Código funcional — documentação gerada retroativamente

---

## O que o produto faz

Monorepo de uma plataforma de pedidos construída com microsserviços independentes. O objetivo principal é demonstrar padrões reais de engenharia de software: mensageria assíncrona com RabbitMQ, separação de responsabilidades entre serviços, testes BDD ponta-a-ponta e trilhas de experimentação com IA/LLM para inteligência no pipeline de pedidos.

## Para quem é

Desenvolvedor (uso educacional/portfólio) que quer demonstrar domínio de: arquitetura de microsserviços, mensageria AMQP, Clean Architecture em Java, workers assíncronos em Python e experimentos com ML/LLM em contexto de negócio real.

## Problema principal resolvido

Demonstrar, num repositório coeso e bem documentado, que o candidato sabe construir sistemas distribuídos com múltiplas linguagens, mensageria assíncrona e integração de IA — todos os componentes trabalhando juntos.

## Stack decidida

- order-service: Java 17 + Spring Boot + AMQP (RabbitMQ client)
- scheduler-agent: Python 3.11 + pika (AMQP) + requests
- Testes BDD: Node.js 18 + Cucumber + TypeScript
- Experimentos ML: Python + scikit-learn / LangChain
- Infra: Docker Compose + RabbitMQ management

## Componentes identificados

1. **order-service** — API HTTP que cria pedidos e publica eventos
2. **scheduler-agent** — worker que consome eventos e confirma pedidos
3. **tests/bdd** — cenários E2E com Cucumber
4. **ml/experiments** — notebooks de previsão de demanda e detecção de anomalias
5. **ml/llm** — experimentos com LLMs para análise de pedidos
6. **infra/terraform** — ponto de partida para IaC

## Fluxos principais

### Fluxo principal: Criar e confirmar pedido
1. Cliente faz `POST /orders` com `{ productId, quantity }`
2. order-service cria pedido (in-memory), retorna `{ id }`
3. order-service publica evento `order.created` no RabbitMQ
4. scheduler-agent consome o evento
5. scheduler-agent chama `POST /orders/{id}/confirm` no order-service
6. Pedido muda de status para confirmado

### Fluxo BDD
1. Cenário Cucumber descreve criação de pedido e verificação de mensagem
2. Steps em TypeScript chamam a API e verificam o RabbitMQ

## Restrições identificadas

- In-memory por enquanto (sem banco de dados real) — roadmap inclui PostgreSQL/Mongo
- Ambiente de laboratório (sem produção configurada)
- Sem autenticação/autorização no MVP atual
