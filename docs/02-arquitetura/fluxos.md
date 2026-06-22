# Fluxos Principais — microservice-shop

## Fluxo 1: Criar e confirmar pedido (fluxo principal)

**Ator:** Cliente REST (ou step BDD)  
**Pré-condição:** Stack rodando (RabbitMQ + order-service + scheduler-agent)

**Passos:**
1. `POST /orders` com `{ "productId": "SKU-1", "quantity": 2 }`
2. order-service valida payload
3. order-service cria Order com status `CREATED`, armazena in-memory
4. order-service publica `{ id, productId, quantity, status }` em `order.exchange` com routing key `order.created`
5. Retorna `201 Created` com `{ "id": "..." }`
6. RabbitMQ entrega mensagem na fila `order.created`
7. scheduler-agent consome a mensagem
8. scheduler-agent chama `POST /orders/{id}/confirm`
9. order-service atualiza status → `CONFIRMED`
10. scheduler-agent faz ack da mensagem

**Pós-condição:** Pedido com status CONFIRMED; mensagem consumida

**Erros:**
- Payload inválido → 400
- RabbitMQ indisponível → 500 (order-service), mensagem reentregue (scheduler-agent)
- order-service indisponível no momento da confirmação → scheduler-agent faz nack, mensagem reentregue

---

## Fluxo 2: Teste BDD ponta-a-ponta

**Ator:** Suite de testes Cucumber  
**Pré-condição:** Stack completa rodando

**Passos:**
1. Cucumber step executa `POST /orders`
2. Step verifica 201 e extrai `id`
3. Step aguarda mensagem em `order.created` via RabbitMQ
4. Step verifica que o pedido foi confirmado (`GET /orders/{id}` ou verificação direta)

**Pós-condição:** Cenário marcado como passed

---

## Fluxo 3: Experimento ML (demand-forecasting)

**Ator:** Desenvolvedor executando notebook/script

**Passos:**
1. `python train.py` — treina modelo com `data/sample_demand.csv`
2. Modelo serializado salvo localmente
3. `python infer.py` — carrega modelo, gera previsão para período futuro
4. Output: previsão de demanda por produto

---

## Contratos de API

### POST /orders
```json
Request:  { "productId": "string", "quantity": number }
Response: { "id": "string" }
Status:   201 Created
```

### POST /orders/{id}/confirm
```json
Request:  (sem body)
Response: 200 OK
```

### GET /actuator/health
```json
Response: { "status": "UP" }
Status:   200 OK
```
