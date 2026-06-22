# Restrições Técnicas — microservice-shop

## Stack por componente (não mudar sem ADR)

| Componente | Tecnologia | Versão mínima |
|-----------|-----------|--------------|
| order-service | Java + Spring Boot | 17 / 3.x |
| scheduler-agent | Python | 3.11 |
| Testes BDD | Node.js + TypeScript + Cucumber | 18 / 10+ |
| Mensageria | RabbitMQ | 3 |
| Infra local | Docker + Docker Compose | v2 |
| Experimentos ML | Python + scikit-learn | 3.11 |

## Restrições de design

- Cada serviço tem seu próprio Dockerfile — sem dependências entre imagens
- Serviços se comunicam APENAS por HTTP (para respostas síncronas) e AMQP (para eventos)
- O banco de dados (quando adicionado) é privado ao serviço que o possui — nenhum outro serviço acessa diretamente
- Diagramas adicionados à documentação devem ser Mermaid (sem imagens binárias no repositório)

## Restrições de custo/infra

- Ambiente local apenas — Docker Compose gratuito
- Experimentos ML: dados de exemplo incluídos no repo (sem dependência de datasets externos)

## O que o AI NÃO deve fazer sem perguntar

- [ ] Adicionar banco de dados sem atualizar também a documentação de arquitetura
- [ ] Criar dependência entre serviços que não seja por HTTP/AMQP
- [ ] Mover lógica de negócio do order-service para o scheduler-agent
- [ ] Colocar código de ML/LLM nos serviços de produção
