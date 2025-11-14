ORDER_SERVICE_DIR := services/api/order-service
SCHEDULER_DIR := services/workers/scheduler-agent
BDD_DIR := tests/bdd
LLM_DIR := ml/llm

.PHONY: help compose-up compose-down compose-logs api-build api-test api-run         worker-install worker-run bdd-install bdd-test llm-setup llm-notebook

help:
	@echo "Targets disponíveis:"
	@echo "  compose-up       - Sobe toda a stack com Docker Compose"
	@echo "  compose-down     - Derruba a stack e remove volumes"
	@echo "  compose-logs     - Segue os logs de todos os serviços"
	@echo "  api-build        - Executa mvn clean install no order-service"
	@echo "  api-test         - Executa mvn test no order-service"
	@echo "  api-run          - Sobe o order-service localmente"
	@echo "  worker-install   - Instala dependências do scheduler-agent"
	@echo "  worker-run       - Inicia o scheduler-agent localmente"
	@echo "  bdd-install      - Instala dependências dos testes BDD"
	@echo "  bdd-test         - Executa npm test em tests/bdd"
	@echo "  llm-setup        - Instala dependências dos notebooks de IA/LLM"
	@echo "  llm-notebook     - Abre Jupyter Lab apontando para ml/llm/notebooks"

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

api-run:
	mvn -f $(ORDER_SERVICE_DIR)/pom.xml spring-boot:run

worker-install:
	pip install -r $(SCHEDULER_DIR)/requirements.txt

worker-run:
	python $(SCHEDULER_DIR)/app.py

bdd-install:
	cd $(BDD_DIR) && npm install

bdd-test:
	cd $(BDD_DIR) && npm test

llm-setup:
	pip install -r $(LLM_DIR)/requirements.txt

llm-notebook:
	cd $(LLM_DIR) && jupyter lab notebooks
