# Estratégia de Git — microservice-shop

## Branches

- `main` — código estável, protegido via PR
- `feat/<descricao>` — nova funcionalidade (ex: `feat/catalog-cache`)
- `fix/<descricao>` — correção de bug (ex: `fix/order-retry`)
- `docs/<descricao>` — somente documentação
- `chore/<descricao>` — setup, dependências, CI

## Commits

Conventional Commits semânticos:
```
feat(order-service): adiciona endpoint de confirmação de pedido
fix(scheduler-agent): corrige reconexão após falha no RabbitMQ
docs: atualiza arquitetura com diagrama Mermaid
test(bdd): adiciona cenário de pedido com quantidade zero
chore: atualiza dependências do Spring Boot
```

## Pull Requests

- PRs pequenos (≤ 400 linhas de diff)
- Descrever: objetivo, testes executados, impacto em deploy/infra
- CI deve passar antes do merge
- Squash merge em `main`

## CI

GitHub Actions em `.github/workflows/ci.yml` — lint + testes + build por serviço.
