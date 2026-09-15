# Supermarket Prices API

API REST para comparação de preços de supermercados, desenvolvida com **Java 21 e Spring Boot**.

O sistema permite consultar produtos, acompanhar preços por cidade e comparar o valor de listas de compras entre estabelecimentos.

## Funcionalidades

- Cadastro e autenticação de usuários
- Consulta de supermercados e produtos
- Histórico de preços por produto e loja
- Criação e gerenciamento de listas de compras
- Comparação de preços por cidade
- Cálculo de valores conforme as quantidades da lista
- Validação de preços expirados e promoções
- Controle de acesso às listas pelo proprietário

## Tecnologias

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- Jakarta Bean Validation
- PostgreSQL
- Flyway
- Maven
- OpenAPI

## Estado atual

O backend possui a estrutura de catálogo, listas e comparação implementada. A integração com fontes externas de preços ainda não está ativa.

O banco inicia com **Rio de Janeiro, Volta Redonda, Barra Mansa e Resende**, sem lojas, produtos ou preços comerciais pré-cadastrados.

Detalhes sobre integração em [Fontes de dados](docs/data-sources.md).

## Executando localmente

Requisitos: **Java 21**, **Git** e **Docker com Compose**.

Clone o projeto:

```bash
git clone https://github.com/Tutzdev/SupermarketPrices.git
cd SupermarketPrices
```

Copie `.env.example` para `.env`, preencha `DATABASE_PASSWORD` e inicie o PostgreSQL:

```bash
docker compose --env-file .env up -d --wait
```

Configure as variáveis da aplicação. No PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'development'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/prices'
$env:DATABASE_USERNAME = 'prices'
$env:DATABASE_PASSWORD = '<mesma senha configurada no .env>'

.\mvnw.cmd spring-boot:run
```

No Linux ou macOS, exporte as mesmas variáveis e execute `./mvnw spring-boot:run`.

O Spring Boot não carrega o `.env` automaticamente. As migrations são aplicadas pelo Flyway na inicialização.

## API

Principais endpoints:

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/products
GET  /api/v1/stores
GET  /api/v1/prices?productId={id}&storeId={id}
POST /api/v1/shopping-lists
GET  /api/v1/comparisons/products?productId={id}&cityId={id}
GET  /api/v1/comparisons/shopping-lists/{id}?cityId={id}
```

Rotas privadas utilizam `Authorization: Bearer <token>`.

Documentação OpenAPI disponível em **http://localhost:8080/v3/api-docs** durante o desenvolvimento. O projeto não inclui frontend nem Swagger UI.

## Testes

```bash
./mvnw test
```

No Windows, utilize `.\mvnw.cmd test`.

Os testes de integração utilizam PostgreSQL real. Disponibilize os binários pelo `PATH` ou `PG_BIN`, ou configure um banco dedicado com `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` e `TEST_DATABASE_PASSWORD`.

## Autor

Desenvolvido por [Tutzdev](https://github.com/Tutzdev).
