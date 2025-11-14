ORDER_SERVICE_DIR := services/api/order-service
SCHEDULER_DIR := services/workers/scheduler-agent
BDD_DIR := tests/bdd
LLM_DIR := ml/llm

.PHONY: help compose-up compose-down compose-logs lint test security         api-build api-test api-run api-lint         worker-install worker-run worker-lint worker-test         bdd-install bdd-test bdd-lint         llm-setup llm-notebook

help:
	@echo "Targets disponíveis:"
	@echo "  compose-up       - Sobe toda a stack com Docker Compose"
	@echo "  compose-down     - Derruba a stack e remove volumes"
	@echo "  compose-logs     - Segue os logs de todos os serviços"
	@echo "  lint             - Executa linters e formatadores de todos os módulos"
	@echo "  test             - Executa testes unitários dos módulos"
	@echo "  security         - Roda checagens de segurança"
	@echo "  api-build        - Executa mvn clean install no order-service"
	@echo "  api-test         - Executa mvn test no order-service"
	@echo "  api-lint         - Executa validação de formato (Spotless) no order-service"
	@echo "  api-run          - Sobe o order-service localmente"
	@echo "  worker-install   - Instala dependências (dev) do scheduler-agent"
	@echo "  worker-run       - Inicia o scheduler-agent localmente"
	@echo "  worker-lint      - Executa black/ruff no scheduler-agent"
	@echo "  worker-test      - Executa pytest no scheduler-agent"
	@echo "  bdd-install      - Instala dependências dos testes BDD"
	@echo "  bdd-test         - Executa npm test em tests/bdd"
	@echo "  bdd-lint         - Executa checagem TypeScript em tests/bdd"
	@echo "  llm-setup        - Instala dependências dos notebooks de IA/LLM"
	@echo "  llm-notebook     - Abre Jupyter Lab apontando para ml/llm/notebooks"

lint: api-lint worker-lint bdd-lint

test: api-test worker-test

security:
	bandit -q -r $(SCHEDULER_DIR)
	cd $(BDD_DIR) && npm audit --omit=dev

compose-up:
	docker compose up -d

compose-down:
	docker compose down -v

compose-logs:
	docker compose logs -f

api-build:
	mvn -f $(ORDER_SERVICE_DIR)/pom.xml clean install

api-test:
	mvn -f $(ORDER_SERVICE_DIR)/pom.xml test

api-lint:
	mvn -f $(ORDER_SERVICE_DIR)/pom.xml spotless:check

api-run:
	mvn -f $(ORDER_SERVICE_DIR)/pom.xml spring-boot:run

worker-install:
	pip install -r $(SCHEDULER_DIR)/requirements-dev.txt

worker-run:
	python $(SCHEDULER_DIR)/app.py

worker-lint:
	black --check $(SCHEDULER_DIR)
	ruff check $(SCHEDULER_DIR)

worker-test:
	PYTHONPATH=$(SCHEDULER_DIR) pytest $(SCHEDULER_DIR)/tests

bdd-install:
	cd $(BDD_DIR) && npm install

bdd-test:
	cd $(BDD_DIR) && npm test

bdd-lint:
	cd $(BDD_DIR) && npm run lint

llm-setup:
	pip install -r $(LLM_DIR)/requirements.txt

llm-notebook:
	cd $(LLM_DIR) && jupyter lab notebooks
