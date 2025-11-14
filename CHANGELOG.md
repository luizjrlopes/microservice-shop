# Changelog

Todos os registros seguem o padrão [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## [0.2.0] - 2024-05-XX
### Added
- `Makefile` com alvos para subir a stack, testar serviços e preparar notebooks IA/LLM.
- Estrutura dedicada para `ml/llm` (notebooks + `requirements.txt`) e `infra/terraform`.
### Changed
- Serviços reposicionados para `services/api/order-service` e `services/workers/scheduler-agent`, com documentação e `docker-compose.yml` atualizados.
- Dependências do scheduler padronizadas com versões fixas em `requirements.txt` e referências ao novo `Makefile` nos guias.

## [0.1.0] - 2024-05-XX
### Added
- README com visão geral, instruções de uso e destaque dos experimentos IA/LLM.
- Guias em `docs/` cobrindo setup, runbook, arquitetura e governança de notebooks.
- `CHANGELOG.md` e `ROADMAP.md` para rastrear progresso e metas.
### Changed
- Documentação alinhada com a stack atual (order-service + scheduler-agent + testes BDD).
### Fixed
- Lacunas de documentação sobre execução e observabilidade reduzidas via novos guias.
