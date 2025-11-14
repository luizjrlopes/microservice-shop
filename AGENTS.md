# AGENTS.md

Estas instruções aplicam-se a todo o repositório `microservice-shop`. Utilize-as sempre que criar, editar ou remover arquivos.

## Convenções de estilo

### Geral
- Priorize clareza e consistência; prefira nomes descritivos e mantenha comentários apenas quando agregam contexto.
- Respeite a linguagem/framework de cada serviço (C#, TypeScript, Java, C++). Use formatadores padrão antes de abrir PR:
  - **.NET**: `dotnet format`.
  - **Node/TypeScript**: `npm run lint` (ou `npm run format`, quando disponível).
  - **Java (Spring Boot)**: `mvn fmt:format` ou `mvn spotless:apply` conforme configurado no `pom.xml`.
  - **C++**: `clang-format` com o estilo padrão da equipe (Google) aplicável a `services/scheduler-cpp`.
- Não adicione blocos `try/catch` ao redor de imports e mantenha o tratamento de erros específico na camada adequada (interfaces → retorno HTTP; application → regras de negócio).

### Documentação
- Atualize `docs/` ou `README.md` relevantes sempre que alterar comportamentos visíveis (novos endpoints, flags de configuração, variáveis de ambiente).
- Cada serviço deve manter exemplos de uso via snippets ou comandos `curl` em sua documentação.
- Adicione diagramas Mermaid apenas quando simplificarem o entendimento (sem imagens binárias no repositório).

## Testes e qualidade

Execute e documente os testes pertinentes antes de abrir PR:
- `dotnet test` em `services/auth-service`.
- `npm test` (ou `npm run test:unit`) em `services/catalog-service`.
- `mvn test` em `services/order-service`.
- `ctest` em `services/scheduler-cpp` se adicionar testes C++.
- `npm test` em `tests/bdd` para cenários Cucumber.
- `k6 run tests/load/checkout-k6.js` para mudanças que afetem desempenho/latência.

Somente aprove mudanças com cobertura mínima:
- Novas features → pelo menos 1 teste automatizado (unitário, integração ou BDD) + atualização de documentação.
- Correções de bug → teste que reproduza o defeito + nota em `docs/changelog.md` (se aplicável).

## Ferramentas e scripts disponíveis
- `docker-compose.yml`: sobe toda a stack local (`docker compose up -d`).
- `services/*/Dockerfile`: use `docker build` para validar imagens específicas.
- `tests/bdd/package.json`: contém scripts `npm run test`, `npm run lint`, `npm run coverage`.
- `docs/` possui templates e exemplos (guia de APIs e arquitetura) para manter a documentação consistente.
- Pipelines de CI (`.github/workflows/*.yml`) executam lint, testes e build; replique localmente antes do push.

## Requisitos mínimos de documentação e testes
1. **Documentação**: toda alteração funcional precisa de uma nota em `README.md` do serviço afetado e, se necessário, em `docs/architecture.md` ou arquivos específicos da API (`openapi/`).
2. **Testes**: não abra PR sem evidências (log de execução ou badge atualizado). Se o recurso não for testável automaticamente, explique a validação manual na descrição do PR.
3. **Observabilidade**: novas métricas/logs devem listar tags e formato esperado.

## Como contribuir
1. **Branches**
   - Base: `main` (imutável via PR).
   - Nomeie branches com o padrão `<tipo>/<descricao-curta>` (ex.: `feat/catalog-cache`, `fix/order-retry`).
2. **Commits**
   - Use convenção semântica: `feat: ...`, `fix: ...`, `chore: ...`, `docs: ...`, `test: ...`.
   - Pequenos commits incrementais, todos formatados e testados.
3. **Pull Requests**
   - Abra PRs pequenos (ideal ≤ 400 linhas de diff).
   - Descreva: objetivo, escopo, testes executados (com comandos) e impacto em deploy/infra.
   - Marque revisores do serviço impactado e aguarde pelo menos 1 aprovação.
   - Não faça merge manual em `main`; utilize o merge padrão do GitHub após CI verde.

Siga estas diretrizes para garantir qualidade, rastreabilidade e colaboração eficiente em todo o monorepo.
