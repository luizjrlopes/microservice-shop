# Infraestrutura como Código

Este diretório reserva espaço para módulos Terraform que irão provisionar filas, bancos e serviços necessários pelo Microservice Shop.

## Convenções
- Estruture subdiretórios por ambiente (`dev`, `staging`, `prod`) ou por domínio (`messaging`, `observability`).
- Exporte variáveis sensíveis via `terraform.tfvars` ignorado pelo Git.
- Padronize backends remotos (S3 + DynamoDB, GCS, etc.) e documente-os em comentários.

Enquanto os módulos não são adicionados, utilize este README para registrar decisões de arquitetura e dependências planejadas.
