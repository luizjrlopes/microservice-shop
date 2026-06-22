# Padrões de Prompt — microservice-shop

## Para adicionar um novo serviço

```
No microservice-shop, crie um novo microsserviço [nome] em [linguagem].
Leia docs/02-arquitetura/visao-tecnica.md e AGENTS.md.
O serviço deve: [lista de responsabilidades].
Comunicação: [HTTP / AMQP — especificar contrato].
Adicione ao docker-compose.yml.
Pronto quando: [critério verificável].
```

## Para adicionar um experimento ML

```
No microservice-shop/ml/experiments/, crie o experimento [nome].
Siga a estrutura de: ml/experiments/demand-forecasting/ como referência.
Dados de exemplo em: data/sample_[nome].csv
Scripts: train.py e infer.py com comentários em português.
```

## Para corrigir um bug no order-service

```
No order-service (Java Spring Boot), o endpoint [X] está retornando [Y] mas deveria [Z].
Arquivo: services/api/order-service/src/main/java/com/shop/order/[caminho]
Não altere a interface OrderRepository — somente a implementação ou o controller.
```
