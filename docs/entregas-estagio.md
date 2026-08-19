# Entregas do Estágio — Registro Histórico

> **Status:** histórico / não canônico.
>
> Este documento registra o contexto em que parte da documentação e das trilhas de IA/ML foi organizada originalmente. Ele não representa mais a narrativa principal, o backlog atual ou o estado técnico vigente do Microservice Shop.

## Por que este arquivo permanece

O repositório preserva este material para rastreabilidade da evolução do projeto. Entretanto, superfícies públicas atuais devem usar como referência:

- [`../README.md`](../README.md);
- [`00-produto/visao-e-objetivo.md`](00-produto/visao-e-objetivo.md);
- [`architecture.md`](architecture.md);
- [`05-evolucao/AUDITORIA_ESTADO_ATUAL.md`](05-evolucao/AUDITORIA_ESTADO_ATUAL.md);
- [`05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md);
- [`../ROADMAP.md`](../ROADMAP.md).

## Contexto histórico resumido

Parte do trabalho anterior foi organizada em torno de três frentes:

- experimentação com IA/ML;
- práticas de MLOps/LLMOps;
- documentação e runbooks.

Essas frentes contribuíram com artefatos que ainda existem, especialmente os experimentos de ML em `ml/experiments` e a documentação operacional. Porém, a evolução atual do projeto está centrada em confiabilidade distribuída: consistência, Outbox, at-least-once, idempotência, retry/DLQ, contratos e observabilidade.

## Estado atual da IA/ML

- previsão de demanda: experimento executável;
- detecção de anomalias: experimento executável;
- LLM: sem feature integrada ao sistema no estado atual.

Por isso, LLM/MLOps não devem ser usados para descrever o núcleo do projeto como se fossem capacidades de produção concluídas.

O histórico detalhado das versões anteriores deste documento permanece acessível pelo Git.