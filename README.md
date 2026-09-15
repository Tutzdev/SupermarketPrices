Supermarket Prices

API REST para comparar preços de supermercados por cidade e calcular o valor de listas de compras.

Funcionalidades

Cadastro e login com autenticação Bearer.

Consulta de produtos, lojas e histórico de preços.

Listas de compras individuais com produtos e quantidades.

Comparação por produto ou lista, considerando validade dos preços e promoções.

Estado atual: backend sem frontend e sem coleta externa ativa. O banco inicia com Rio de Janeiro, Volta Redonda, Barra Mansa e Resende, mas sem lojas, produtos ou preços comerciais. A integração de fontes está descrita em docs/data-sources.md.

Stack

Java 21 · Spring Boot 4.1.1 · Spring Security · Spring Data JPA · PostgreSQL 18 · Flyway · Maven

Executar localmente

Requisitos: JDK 21 e Docker com Compose. Os comandos abaixo usam PowerShell.

git clone https://github.com/Tutzdev/SupermarketPrices.git
cd SupermarketPrices
Copy-Item .env.example .env

Defina DATABASE_PASSWORD no .env e suba o banco:

docker compose --env-file .env up -d --wait

Configure a aplicação com a mesma senha e execute:

$env:SPRING_PROFILES_ACTIVE = 'development'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/prices'
$env:DATABASE_USERNAME = 'prices'
$env:DATABASE_PASSWORD = '<mesma senha do .env>'
.\mvnw.cmd spring-boot:run

O Spring Boot não carrega o .env automaticamente. No Linux/macOS, exporte as mesmas variáveis e use ./mvnw spring-boot:run.

API: http://localhost:8080/api/v1

OpenAPI JSON: localhost:8080/v3/api-docs

Não há página inicial nem Swagger UI. O Flyway aplica as migrations ao iniciar.

Testes

.\mvnw.cmd test

Os testes de integração exigem PostgreSQL real: disponibilize initdb e pg_ctl no PATH (ou informe PG_BIN), ou configure TEST_DATABASE_URL, TEST_DATABASE_USERNAME e TEST_DATABASE_PASSWORD para um banco dedicado a testes, com permissão para criar schemas.
