# Agents.md

Resumo Objetivo :
Objetivo do repositório: Loja exemplo baseada em arquitetura de microsserviços com REST, mensageria e DDD, coberta por BDD (Cucumber.js) e teste de carga (k6), empacotada em Docker e com CI no GitHub Actions.
Serviços (poliglota):

Auth – C# / .NET 8 (Minimal API)

Catalog – Node.js (Fastify + TypeScript)

Order – Java (Spring Boot)

Scheduler – C++ (job worker simulado)

Infra – RabbitMQ, PostgreSQL, MongoDB

🗂️ Estrutura do Monorepo
microservice-shop/
├─ README.md
├─ docker-compose.yml
├─ .github/
│ └─ workflows/
│ ├─ ci.yml
│ ├─ bdd-e2e.yml
│ └─ docker-publish.yml
├─ openapi/
│ ├─ auth.yaml
│ ├─ catalog.yaml
│ └─ order.yaml
├─ deploy/
│ ├─ ansible/
│ │ ├─ inventory.ini
│ │ ├─ site.yml
│ │ └─ roles/docker/tasks/main.yml
│ ├─ chef/
│ │ ├─ cookbooks/docker_demo/metadata.rb
│ │ └─ cookbooks/docker_demo/recipes/default.rb
│ └─ k8s/
│ ├─ catalog-deployment.yaml
│ ├─ order-deployment.yaml
│ ├─ auth-deployment.yaml
│ └─ rabbitmq-statefulset.yaml
├─ tests/
│ ├─ bdd/
│ │ ├─ package.json
│ │ ├─ features/checkout.feature
│ │ └─ features/steps/checkout.steps.ts
│ └─ load/
│ └─ checkout-k6.js
└─ services/
├─ auth-service/ # C# (.NET 8)
│ ├─ AuthService.csproj
│ ├─ Program.cs
│ ├─ Dockerfile
│ └─ src/{domain,application,infrastructure,interfaces}/
├─ catalog-service/ # Node + Fastify + TS
│ ├─ package.json
│ ├─ tsconfig.json
│ ├─ src/index.ts
│ ├─ src/routes/products.ts
│ ├─ Dockerfile
│ └─ src/{domain,application,infrastructure,interfaces}/
├─ order-service/ # Java + Spring Boot
│ ├─ pom.xml
│ ├─ src/main/java/com/shop/order/OrderApplication.java
│ ├─ src/main/java/com/shop/order/api/OrderController.java
│ └─ Dockerfile
└─ scheduler-cpp/ # C++
├─ main.cpp
└─ Dockerfile

🧭 Diagramas de Arquitetura (Mermaid para embutir no README)

Componentes:

flowchart LR
subgraph Client
UI[Frontend/Client]
end

subgraph Services
AUTH[Auth Service (.NET)]
CATALOG[Catalog Service (Node/TS)]
ORDER[Order Service (Spring Boot)]
SCHED[Scheduler (C++)]
end

subgraph Infra
RABBIT[(RabbitMQ)]
PG[(PostgreSQL)]
MONGO[(MongoDB)]
end

UI -->|REST| CATALOG
UI -->|REST| AUTH
UI -->|REST| ORDER

ORDER <-->|Events| RABBIT
SCHED -->|Consumes| RABBIT

AUTH --> PG
CATALOG --> MONGO
ORDER --> PG

Sequência de Checkout (simplificada):

sequenceDiagram
participant U as User
participant C as Catalog
participant O as Order
participant R as RabbitMQ
participant S as Scheduler

U->>C: GET /products
U->>O: POST /orders
O->>R: publish "order.created"
S-->>R: subscribe "order.created"
S->>O: POST /orders/{id}/confirm
O-->>U: 201 Created

🧱 README.md (copie e cole na raiz)

# microservice-shop

Monorepo de **microsserviços** (Auth, Catalog, Order, Scheduler) com **REST**, **DDD**, **BDD** e **teste de carga**, empacotado em **Docker** e com **CI GitHub Actions**.

## ✨ Tecnologias

- **Auth:** .NET 8 Minimal API (C#)
- **Catalog:** Node.js + Fastify + TypeScript
- **Order:** Java + Spring Boot
- **Scheduler:** C++ (worker simulado)
- **Infra:** RabbitMQ, PostgreSQL, MongoDB
- **Testes:** Cucumber.js (BDD), k6 (carga)
- **DevOps:** Docker/Compose, GitHub Actions, Ansible, Chef

## 🏗 Arquitetura

(veja diagramas Mermaid neste README ou na documentação `/docs`)

## 🔧 Pré-requisitos

- Docker + Docker Compose (v2)
- Node 18+ (para testes BDD), Java 17+, .NET 8 SDK (opcional para build local)

## ⚙️ Variáveis de Ambiente

Crie um arquivo `.env` na raiz (exemplo):

POSTGRES_USER=shop
POSTGRES_PASSWORD=shop
POSTGRES_DB=shopdb
MONGO_INITDB_ROOT_USERNAME=shop
MONGO_INITDB_ROOT_PASSWORD=shop
RABBITMQ_DEFAULT_USER=shop
RABBITMQ_DEFAULT_PASS=shop

## ▶️ Como subir tudo (dev)

````bash
docker compose up --build
# Auth:     http://localhost:8081/health
# Catalog:  http://localhost:8082/health
# Order:    http://localhost:8083/actuator/health
# RabbitMQ: http://localhost:15672 (guest/guest ou variáveis .env)

✅ Testes BDD
cd tests/bdd
npm i
npm test

📈 Teste de carga (k6)
cd tests/load
k6 run checkout-k6.js

🧪 Padrões de Qualidade

DDD por serviço: src/{domain,application,infrastructure,interfaces}

BDD em tests/bdd com cenários Gherkin

CI em .github/workflows/ (build, test, docker publish)

OpenAPI em /openapi

🚀 Deploy/Infra

Ansible: provisioning de host com Docker (deploy/ansible)

Chef: cookbook de exemplo (deploy/chef)

Kubernetes (manifestos básicos em deploy/k8s)

📚 Referências oficiais

Fastify (Node) – docs e guides.
fastify.io
+2
fastify.io
+2

.NET Minimal APIs – docs MS.
Microsoft Learn
+1

Spring Boot – docs e guides.
Home
Home
+1

RabbitMQ – docs e tutoriais.
RabbitMQ
+2
RabbitMQ
+2

Docker/Compose – docs e referência.
Docker Documentation
+1
GitHub

Kubernetes – Deployments e kubectl.
Kubernetes
+2
Kubernetes
+2

Ansible / Chef – documentação.
Ansible Documentation
docs.chef.io
devdocs.io
Chef Software

GitHub Actions – documentação.
GitHub Docs

Cucumber.js – instalação e uso.
Cucumber
+1
cucumber.github.io

k6 – docs oficiais.
Grafana Labs
+1
GitHub

OpenAPI – especificação.
swagger.io
+1


---

## 🧩 `docker-compose.yml` (raiz)

```yaml
version: "3.9"
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-shop}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-shop}
      POSTGRES_DB: ${POSTGRES_DB:-shopdb}
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]

  mongo:
    image: mongo:6
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_INITDB_ROOT_USERNAME:-shop}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_INITDB_ROOT_PASSWORD:-shop}
    ports: ["27017:27017"]
    volumes: ["mongodata:/data/db"]

  rabbitmq:
    image: rabbitmq:3-management
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_DEFAULT_USER:-guest}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_DEFAULT_PASS:-guest}
    ports: ["5672:5672", "15672:15672"]

  auth-service:
    build: ./services/auth-service
    ports: ["8081:8080"]
    environment:
      ASPNETCORE_URLS: http://0.0.0.0:8080
      ConnectionStrings__Default: Host=postgres;Port=5432;Database=shopdb;Username=${POSTGRES_USER:-shop};Password=${POSTGRES_PASSWORD:-shop}
    depends_on: [postgres]

  catalog-service:
    build: ./services/catalog-service
    ports: ["8082:8080"]
    environment:
      MONGO_URL: mongodb://$${MONGO_INITDB_ROOT_USERNAME:-shop}:$${MONGO_INITDB_ROOT_PASSWORD:-shop}@mongo:27017
    depends_on: [mongo]

  order-service:
    build: ./services/order-service
    ports: ["8083:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-shopdb}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-shop}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-shop}
      RABBIT_URL: amqp://rabbitmq:5672
    depends_on: [postgres, rabbitmq]

  scheduler-cpp:
    build: ./services/scheduler-cpp
    depends_on: [rabbitmq]
    # Exemplo simplificado; integração com RabbitMQ futura

volumes:
  pgdata:
  mongodata:

🔐 Auth Service – C# (.NET 8 Minimal API)

services/auth-service/Program.cs

using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Hosting;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

app.MapGet("/health", () => Results.Ok(new { status = "ok", service = "auth" }));
app.MapPost("/auth/register", async (HttpContext ctx) => Results.Created("/auth/users/1", new { id = 1 }));
app.MapPost("/auth/login", async (HttpContext ctx) => Results.Ok(new { token = "fake-jwt" }));

app.Run();


services/auth-service/AuthService.csproj

<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
  </PropertyGroup>
</Project>


services/auth-service/Dockerfile

# build
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY . .
RUN dotnet publish -c Release -o /app

# runtime
FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
COPY --from=build /app .
EXPOSE 8080
ENV ASPNETCORE_URLS=http://+:8080
ENTRYPOINT ["dotnet", "AuthService.dll"]


Referência Minimal APIs (.NET).
Microsoft Learn
+1

🛒 Catalog Service – Node.js (Fastify + TS)

services/catalog-service/package.json

{
  "name": "catalog-service",
  "version": "0.1.0",
  "type": "module",
  "main": "dist/index.js",
  "scripts": {
    "dev": "tsc -w & node --watch dist/index.js",
    "build": "tsc",
    "start": "node dist/index.js",
    "lint": "eslint ."
  },
  "dependencies": {
    "fastify": "^5.0.0"
  },
  "devDependencies": {
    "typescript": "^5.4.0",
    "@types/node": "^20.11.0"
  }
}


services/catalog-service/tsconfig.json

{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "moduleResolution": "Node",
    "outDir": "dist",
    "strict": true
  },
  "include": ["src"]
}


services/catalog-service/src/index.ts

import Fastify from 'fastify'
import { productsRoutes } from './routes/products.js'

const app = Fastify()
app.get('/health', async () => ({ status: 'ok', service: 'catalog' }))
app.register(productsRoutes, { prefix: '/products' })

const port = Number(process.env.PORT ?? 8080)
app.listen({ port, host: '0.0.0.0' })


services/catalog-service/src/routes/products.ts

import { FastifyInstance } from 'fastify'

export async function productsRoutes(app: FastifyInstance) {
  app.get('/', async () => ([
    { id: 'p1', name: 'Keyboard', price: 199.9 },
    { id: 'p2', name: 'Mouse', price: 99.9 }
  ]))
}


services/catalog-service/Dockerfile

FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY tsconfig.json ./
COPY src ./src
RUN npm run build

FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY package*.json ./
RUN npm ci --omit=dev
EXPOSE 8080
CMD ["node", "dist/index.js"]


Referências Fastify.
fastify.io
+2
fastify.io
+2

📦 Order Service – Java (Spring Boot)

services/order-service/pom.xml (trecho essencial)

<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.shop</groupId>
  <artifactId>order-service</artifactId>
  <version>0.0.1</version>
  <properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.2</spring-boot.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type><scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
    </plugins>
  </build>
</project>


services/order-service/src/main/java/com/shop/order/OrderApplication.java

package com.shop.order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderApplication {
  public static void main(String[] args) { SpringApplication.run(OrderApplication.class, args); }
}


services/order-service/src/main/java/com/shop/order/api/OrderController.java

package com.shop.order.api;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
  record OrderDTO(String id, String productId, int qty, String status) {}
  @PostMapping public OrderDTO create(@RequestBody Map<String,Object> body) {
    return new OrderDTO(UUID.randomUUID().toString(), (String) body.get("productId"), (int) body.getOrDefault("qty",1), "CREATED");
  }
}


services/order-service/Dockerfile

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]


Referências Spring Boot.
Home
Home
+1

⏱️ Scheduler – C++ (worker simples)

services/scheduler-cpp/main.cpp

#include <chrono>
#include <iostream>
#include <thread>

int main() {
  std::cout << "[scheduler] started (demo)\n";
  // TODO: integrar com RabbitMQ (AMQP) para consumir "order.created"
  while (true) {
    std::this_thread::sleep_for(std::chrono::seconds(10));
    std::cout << "[scheduler] tick - polling placeholder\n";
  }
  return 0;
}


services/scheduler-cpp/Dockerfile

FROM alpine:3.20 AS build
RUN apk add --no-cache g++
WORKDIR /src
COPY main.cpp .
RUN g++ -O2 -static -o app main.cpp

FROM scratch
COPY --from=build /src/app /app
ENTRYPOINT ["/app"]

📑 OpenAPI (exemplo – openapi/catalog.yaml)
openapi: 3.0.3
info:
  title: Catalog API
  version: 0.1.0
paths:
  /products:
    get:
      summary: List products
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                type: array
                items:
                  type: object
                  properties:
                    id: { type: string }
                    name: { type: string }
                    price: { type: number }


Especificação OpenAPI.
swagger.io

🧪 BDD – Cucumber.js

tests/bdd/package.json

{
  "name": "bdd-tests",
  "version": "0.1.0",
  "type": "module",
  "scripts": { "test": "cucumber-js" },
  "dependencies": { "@cucumber/cucumber": "^10.0.0", "node-fetch": "^3.3.2" }
}


tests/bdd/features/checkout.feature

Feature: Checkout
  As a user I want to create an order so that I can buy a product

  Scenario: Create order from existing product
    Given the catalog has products
    When I create an order with productId "p1" and qty 1
    Then the order status should be "CREATED"


tests/bdd/features/steps/checkout.steps.ts

import { Given, When, Then } from '@cucumber/cucumber'
import fetch from 'node-fetch'
import assert from 'node:assert'

let response: any;

Given('the catalog has products', async function () {
  const res = await fetch('http://localhost:8082/products')
  const products = await res.json()
  assert.ok(Array.isArray(products) && products.length > 0)
})

When('I create an order with productId {string} and qty {int}', async function (pid: string, qty: number) {
  const res = await fetch('http://localhost:8083/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ productId: pid, qty })
  })
  response = await res.json()
})

Then('the order status should be {string}', function (status: string) {
  assert.equal(response.status, status)
})


Cucumber.js (instalação e uso).
Cucumber
+1

📈 Teste de Carga – k6

tests/load/checkout-k6.js

import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  vus: 20, duration: '30s',
  thresholds: { http_req_duration: ['p(95)<500'] }
}

export default function () {
  const products = http.get('http://localhost:8082/products')
  check(products, { '200 products': r => r.status === 200 })

  const order = http.post('http://localhost:8083/orders', JSON.stringify({ productId: 'p1', qty: 1 }), {
    headers: { 'Content-Type': 'application/json' }
  })
  check(order, { '201/200 order': r => r.status === 200 || r.status === 201 })
  sleep(1)
}


k6 – documentação.
Grafana Labs

🔁 GitHub Actions – CI/CD

.github/workflows/ci.yml (build + testes)

name: CI
on:
  push: { branches: [ main ] }
  pull_request: { branches: [ main ] }
jobs:
  build-test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [auth-service, catalog-service, order-service, scheduler-cpp]
    steps:
      - uses: actions/checkout@v4
      - name: Build ${{ matrix.service }}
        run: |
          case "${{ matrix.service }}" in
            auth-service) docker build -t auth ./services/auth-service ;;
            catalog-service) docker build -t catalog ./services/catalog-service ;;
            order-service) docker build -t order ./services/order-service ;;
            scheduler-cpp) docker build -t scheduler ./services/scheduler-cpp ;;
          esac
      - name: BDD (dockerized)
        if: matrix.service == 'scheduler-cpp'
        run: |
          docker compose up -d
          cd tests/bdd && npm ci && npm test


.github/workflows/bdd-e2e.yml (executa BDD em PR)

name: BDD E2E
on:
  pull_request:
    branches: [ main ]
jobs:
  bdd:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker compose up -d --build
      - run: cd tests/bdd && npm ci && npm test


GitHub Actions – documentação oficial.
GitHub Docs

.github/workflows/docker-publish.yml (publicar imagens no GHCR em tag)

name: Docker Publish
on:
  push:
    tags: ['v*.*.*']
jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - name: Login GHCR
        run: echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
      - name: Build & Push
        run: |
          for svc in auth-service catalog-service order-service scheduler-cpp; do
            docker build -t ghcr.io/${{ github.repository_owner }}/$svc:${{ github.ref_name }} ./services/$svc
            docker push ghcr.io/${{ github.repository_owner }}/$svc:${{ github.ref_name }}
          done

🛠️ Infra como Código
Ansible – provisionamento Docker no host

deploy/ansible/inventory.ini

[shop]
your-ec2-ip ansible_user=ubuntu


deploy/ansible/site.yml

- hosts: shop
  become: yes
  roles: [docker]


deploy/ansible/roles/docker/tasks/main.yml

- name: Install packages
  apt:
    name: [apt-transport-https, ca-certificates, curl, gnupg, lsb-release]
    state: present
    update_cache: yes
- name: Add Docker GPG key
  shell: curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker.gpg
  args: { creates: /usr/share/keyrings/docker.gpg }
- name: Add Docker repo
  copy:
    dest: /etc/apt/sources.list.d/docker.list
    content: "deb [arch=amd64 signed-by=/usr/share/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu focal stable"
- name: Install Docker+Compose
  apt:
    name: [docker-ce, docker-ce-cli, containerd.io, docker-compose-plugin]
    state: present
    update_cache: yes
- name: Deploy stack
  copy: { src: "{{ playbook_dir }}/../../docker-compose.yml", dest: /opt/shop/docker-compose.yml }
- name: Up compose
  shell: docker compose up -d


Ansible docs.
Ansible Documentation

Chef – cookbook simples para instalar Docker

deploy/chef/cookbooks/docker_demo/metadata.rb

name 'docker_demo'
version '0.1.0'
depends 'docker'


deploy/chef/cookbooks/docker_demo/recipes/default.rb

package 'apt-transport-https'
package 'ca-certificates'
package 'curl'
# ... (exemplo reduzido)
execute 'install_docker' do
  command 'curl -fsSL https://get.docker.com | sh'
  not_if 'which docker'
end


Chef Infra (overview).
docs.chef.io
Chef Software

📊 Kubernetes (exemplo mínimo – deploy/k8s/catalog-deployment.yaml)
apiVersion: apps/v1
kind: Deployment
metadata: { name: catalog }
spec:
  replicas: 2
  selector: { matchLabels: { app: catalog } }
  template:
    metadata: { labels: { app: catalog } }
    spec:
      containers:
        - name: catalog
          image: ghcr.io/ORG/catalog-service:latest
          ports: [{ containerPort: 8080 }]
---
apiVersion: v1
kind: Service
metadata: { name: catalog-svc }
spec:
  selector: { app: catalog }
  ports: [{ port: 80, targetPort: 8080 }]


Deployments e kubectl.
Kubernetes
+1

🧠 DDD dentro de cada serviço

src/domain: entidades, value objects, agregados (Order, Product, User)

src/application: casos de uso (CreateOrder, ListProducts)

src/infrastructure: repositórios (PostgreSQL/Mongo), mensageria (Rabbit)

src/interfaces: controllers REST, DTOs, validações

Benefício: separa regras de negócio da infraestrutura, facilitando testes e escalabilidade.

🧪 BDD & Qualidade – Boas Práticas

Escrever cenários Gherkin focados em comportamento de negócio.

Mapear cenários → casos de uso (camada application).

Relatórios (opcional): Allure integrado ao Cucumber.
allurereport.org

📈 Desempenho & Escalabilidade

k6 thresholds (p95 < 500ms) no script.

Cache (futuro): Redis em frente ao Catalog.

Horizontal scaling: replicas nos manifests do K8s.

Observabilidade (futuro): Prometheus/Grafana.

Dynatrace (opcional): anexar OneAgent no runtime/container (seguir docs do OneAgent).
RabbitMQ

🧰 GitHub – Fluxo de Trabalho Sugerido

Commits semânticos (feat:, fix:, chore:)

PR template com checklist (build, testes, BDD ok)

Releases versionadas (v0.1.0) → workflow docker-publish.yml publica imagens

Projects para roadmap/kanban

🗺️ Backlog Inicial (Issues sugeridas)

catalog-service: persistir produtos no MongoDB (infra)

order-service: publicar evento order.created no RabbitMQ

scheduler-cpp: consumir eventos de order.created (AMQP C++ client)

auth-service: JWT real + hashing de senha

BDD: cenários de falha (produto inexistente, qty inválida)

k6: testes por picos (spike) e teste de saturação (soak)

Kubernetes: adicionar HorizontalPodAutoscaler
````
