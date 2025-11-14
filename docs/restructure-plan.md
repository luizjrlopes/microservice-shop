# Microservice Shop – Inventário e Plano de Reestruturação

## Mapa de Diretórios (profundidade 2)
```
microservice-shop/
├── docker-compose.yml
├── services/
│   ├── api/
│   │   └── order-service/
│   └── workers/
│       └── scheduler-agent/
├── ml/
│   └── llm/
│       ├── notebooks/
│       └── requirements.txt
├── infra/
│   └── terraform/
└── tests/
    └── bdd/
```
_Estrutura inspecionada a partir da raiz do repositório._

## Estrutura-alvo padronizada
| Pilar | Caminho | Observações |
| --- | --- | --- |
| APIs síncronas | `services/api/<serviço>` | Abriga microsserviços HTTP (ex.: `order-service`). |
| Workers/eventos | `services/workers/<serviço>` | Processos orientados a filas, como `scheduler-agent`. |
| IA/LLM | `ml/llm` | Centraliza notebooks, dependências e utilitários experimentais. |
| Infraestrutura | `infra/terraform` | Repositório para módulos IaC futuros. |

O `Makefile` na raiz referencia esses caminhos para build/teste, garantindo nomenclatura consistente e scripts reutilizáveis.

## Inventário dos Módulos
| Módulo | Linguagem | Finalidade | Dependências principais | Status de testes |
| --- | --- | --- | --- | --- |
| `services/api/order-service` | Java 17 + Spring Boot | API REST para criar e confirmar pedidos, além de publicar eventos `order.created` no RabbitMQ. | `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-amqp`, `jackson-databind`. | `mvn test` concluiu sem testes (`No tests to run`). |
| `services/workers/scheduler-agent` | Python 3.11 | Worker que consome a fila `order.created` e chama `POST /orders/{id}/confirm` no serviço de pedidos. | `pika`, `requests`. | Não há testes automatizados definidos. |
| `tests/bdd` | TypeScript + Cucumber.js | Suíte BDD cobrindo criação de pedidos e publicação de eventos. | `@cucumber/cucumber`, `ts-node`, `typescript`, `axios`, `amqplib`. | Não executado (exige RabbitMQ e order-service ativos). |

## Problemas e Riscos Detectados
1. **Serviços planejados ausentes** – Somente `order-service` e `scheduler-agent` existem no monorepo; `auth-service`, `catalog-service` e demais componentes citados na documentação não foram versionados.
2. **Scheduler fora da stack planejada** – O worker foi implementado em Python em vez de C++ como definido no objetivo inicial, não havendo testes ou build que garantam seu comportamento.
3. **Order-service sem cobertura automatizada** – A execução de `mvn test` finaliza sem qualquer caso, deixando lógicas críticas (validação, publicação em RabbitMQ) sem regressão.
4. **BDD depende de infraestrutura externa** – A suíte usa axios/amqplib apontando para `localhost` sem mocks; não há scripts para subir dependências antes dos testes, dificultando sua execução em CI.
5. **`node_modules` commitado em `tests/bdd`** – Dependências instaladas estão versionadas, o que aumenta o repositório e causa risco de divergência com `package-lock.json`.
