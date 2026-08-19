# Visão e Objetivo — Microservice Shop

## Problema

Processamento assíncrono de pedidos parece simples enquanto todos os componentes estão disponíveis. O problema real aparece quando banco, broker, rede ou consumidor falham em momentos diferentes: eventos podem se perder, mensagens podem ser entregues novamente, efeitos podem ser duplicados e o estado final pode se tornar difícil de rastrear.

## Solução

O Microservice Shop modela um fluxo de pedidos orientado a eventos em uma stack poliglota e mantém o escopo deliberadamente pequeno para tornar as propriedades distribuídas observáveis e testáveis de ponta a ponta.

O sistema combina:

- API de pedidos em Java/Spring Boot;
- mensageria RabbitMQ;
- worker assíncrono em Python;
- execução local com Docker Compose;
- testes Java, Python e BDD;
- documentação de decisões arquiteturais;
- uma trilha experimental separada de ML.

## Objetivo principal

Construir um fluxo de processamento de pedidos que possa demonstrar, por implementação e testes, propriedades como:

- consistência entre estado persistido e evento;
- entrega at-least-once;
- idempotência;
- retry e Dead Letter Queue;
- contratos de evento versionados;
- rastreabilidade entre processos;
- recuperação de falhas transitórias;
- CI capaz de provar o ciclo distribuído completo.

## Princípio de escopo

**Profundidade antes de quantidade.**

O projeto não adiciona um novo microsserviço apenas para aumentar o diagrama. Uma nova fronteira distribuída só entra quando possui uma responsabilidade própria e aumenta a qualidade da tese arquitetural.

Por isso, catálogo, autenticação separada, pagamentos, frontend, Kubernetes e um serviço LLM não fazem parte do núcleo atual.

## Estado atual e estado-alvo

### Atual

```text
Client
  -> order-service
      -> in-memory repository
      -> RabbitMQ
          -> scheduler-agent
              -> order-service /confirm
```

### Evolução aprovada

```text
Client
  -> order-service
      -> PostgreSQL + Transactional Outbox
          -> RabbitMQ
              -> scheduler-agent
                  -> retry / DLQ
                  -> confirmação idempotente
```

A evolução completa está documentada em [`../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).

## O que o projeto não afirma

No estado atual, o projeto não se apresenta como:

- e-commerce completo;
- solução pronta para produção;
- arquitetura cloud já provisionada;
- plataforma de LLM;
- sistema com exactly-once delivery.

O objetivo é ser tecnicamente preciso: cada capacidade pública deve ser sustentada por código, configuração ou teste reproduzível.