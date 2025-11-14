# Roadmap

Visão macro das próximas entregas para o Microservice Shop. As datas são estimativas e podem mudar conforme descobertas.

## Q2 2024 – Fundamentos
1. **Cobertura de testes mínima** no `order-service` (unitários + integração AMQP).
2. **Refinamento do scheduler**: substituir lógica síncrona por confirmações idempotentes e adicionar telemetria básica.
3. **Infra de dados para IA**: padronizar export de eventos e pipelines de dados compartilhados com notebooks (`data/`).

## Q3 2024 – Novos serviços e IA aplicada
1. **Catalog-service** (Node/TypeScript) expondo `GET /products` e cache em Redis.
2. **Auth-service** (ASP.NET Core) fornecendo JWT para proteger `order-service`.
3. **AI Advisor**: microsserviço dedicado a recomendações, alimentado pelos notebooks de LLM; inicialmente expõe `POST /advice/orders` retornando explicações textuais.

## Q4 2024 – Observabilidade e automação
1. **Tracing distribuído** com OpenTelemetry em todos os serviços.
2. **Playbooks gerados por LLM** integrados ao scheduler (sugestões de recuperação diretamente nos logs).
3. **Pipeline CI/CD** executando notebooks críticos em modo headless para garantir reprodutibilidade.

## Backlog estratégico
- Persistência real (PostgreSQL) para pedidos e histórico de eventos.
- Suporte a múltiplas filas (dead-letter, retries) com políticas configuráveis.
- Dashboards que combinam métricas tradicionais e insights dos modelos generativos.

Acompanhe mudanças confirmadas no [`CHANGELOG.md`](CHANGELOG.md) e utilize issues/PRs para propor replanejamentos.
