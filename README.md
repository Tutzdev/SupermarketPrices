# Supermarket Prices API

Backend REST para comparação de preços de supermercados por cidade e por lista de compras. Escopo inicial: Rio de Janeiro/RJ, Volta Redonda, Barra Mansa e Resende. Essas localidades são dados de uma migration, não condições na regra de negócio.

Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.16, Spring MVC, Data JPA, Security, Bean Validation, PostgreSQL, Flyway e Lombok. OpenAPI JSON gerado por springdoc 3.1.1, sem interface web.

## Estado dos dados

**Nenhuma loja, rede, produto ou preço comercial é pré-cadastrado.** Não há coletor externo ativo nem fonte comercial homologada nesta entrega. Catálogos vazios e comparações sem observação são resultados esperados até integrar uma fonte verificável. Fixtures fictícias ficam exclusivamente em `src/test/` e nunca entram no JAR de produção.

## Executar

Requisitos: JDK 21 ou superior compatível com Spring Boot, PostgreSQL 18 e acesso ao Maven Central no primeiro build. O código é compilado com `--release 21`. O Maven Wrapper dispensa instalação global de Maven.

1. Copie `.env.example` para `.env` e defina uma senha local. `.env` é ignorado pelo Git.
2. Para criar o PostgreSQL local com Docker, execute `docker compose --env-file .env up -d`. Alternativamente, crie um banco `prices` em sua instalação de PostgreSQL e um usuário proprietário desse banco.
3. Exporte as variáveis no processo que iniciará a aplicação. **Spring Boot e Maven não carregam `.env` automaticamente**; o Compose o utiliza apenas para o banco.

Exemplo em PowerShell, substituindo a senha pela configurada no banco:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'development'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/prices'
$env:DATABASE_USERNAME = 'prices'
$env:DATABASE_PASSWORD = '<senha configurada no PostgreSQL>'
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS, exporte as mesmas variáveis e execute `./mvnw spring-boot:run`. A API escuta em `http://localhost:8080`. Não há página inicial.

Para empacotar, execute `./mvnw clean verify` e inicie `java -jar target/prices-0.0.1-SNAPSHOT.jar` com as variáveis exportadas.

## Configuração

| Variável | Uso / padrão |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `development` ou `production`; não há perfil de desenvolvimento implícito |
| `DATABASE_URL` | URL JDBC PostgreSQL; obrigatória fora de development |
| `DATABASE_USERNAME` | Usuário do banco; obrigatório fora de development |
| `DATABASE_PASSWORD` | Senha do banco, sempre externa |
| `DATABASE_POOL_SIZE` | Conexões máximas, padrão 10 |
| `PORT` | Porta HTTP, padrão 8080 |
| `CORS_ALLOWED_ORIGINS` | Origens HTTP(S) exatas separadas por vírgula, sem caminho; vazio bloqueia acesso entre origens |
| `AUTH_TOKEN_TTL` | Duração ISO-8601, padrão `PT12H` |
| `PRICE_MAX_AGE` | Limite de idade de uma observação, padrão `P2D` (48 horas) |
| `OPENAPI_ENABLED` | `true` em desenvolvimento; `false` por padrão em produção |

Em produção, forneça a configuração de conexão e TLS adequada à sua infraestrutura. A senha do banco não tem valor padrão. CORS não aceita curingas; a API autentica por cabeçalho Bearer e não usa cookies de sessão.

## Banco e migrations

Flyway aplica automaticamente `src/main/resources/db/migration/` na inicialização. Hibernate apenas valida o schema (`ddl-auto=validate`); `open-in-view` está desabilitado.

- V1: usuários e hashes de tokens.
- V2: localidades, fontes, redes, unidades, produtos e referências externas.
- V3: estado RJ e as quatro cidades autorizadas.
- V4: observações históricas de preços e índices para última observação.
- V5: listas e itens.
- V6: versões para impedir sobrescritas concorrentes de observações do catálogo.

Mudanças posteriores devem usar novas migrations. Não edite uma migration que já foi aplicada. A exclusão de uma lista remove apenas seus itens; referências de preços impedem apagar seu catálogo associado.

## Autenticação

`POST /api/v1/auth/register` recebe `{ "name": "...", "email": "...", "password": "..." }` e retorna o perfil com status 201. E-mails são normalizados e únicos no banco. Senhas precisam de pelo menos 12 caracteres e no máximo 72 bytes UTF-8; são armazenadas com BCrypt, custo 12.

`POST /api/v1/auth/login` recebe `{ "email": "...", "password": "..." }` e retorna `accessToken`, `tokenType`, `expiresAt` e `user`. Use `Authorization: Bearer <accessToken>` nas rotas protegidas. O token é aleatório de 256 bits; somente seu hash SHA-256 é persistido. A autenticação consulta expiração no banco. Login não renova outros tokens; não há refresh token.

`POST /api/v1/auth/logout` revoga o token apresentado e retorna 204. `GET /api/v1/users/me` consulta o perfil e `PATCH /api/v1/users/me`, com `{ "name": "..." }`, altera o nome. O backend não cria usuário administrativo.

## Endpoints

Prefixo consistente: `/api/v1`. IDs são UUIDs. Timestamps são instantes ISO-8601 em UTC. Respostas não expõem entidades JPA.

| Método e caminho | Entrada / comportamento |
| --- | --- |
| `GET /states` | Estados cadastrados |
| `GET /cities` | Filtro opcional `stateId` |
| `GET /chains` | Redes com origem identificada |
| `GET /stores` | Unidades ativas, filtro opcional `cityId` |
| `GET /stores/{id}` | Detalhes de uma unidade, incluindo atividade e origem |
| `GET /products` | Busca `query`, filtros `brand`, `gtin`, `category` |
| `GET /products/{id}` | Produto normalizado e metadados de origem |
| `GET /prices` | Histórico; exige `productId` e `storeId`, mais recente primeiro |
| `GET /comparisons/products` | Exige `productId` e `cityId`; preços por loja |
| `GET /shopping-lists` | Listas do usuário autenticado, sem carregar itens de todas as listas |
| `POST /shopping-lists` | `{ "name": "...", "shoppingType": "WEEKLY" }`; retorna 201 |
| `GET /shopping-lists/{id}` | Lista do proprietário com itens |
| `PUT /shopping-lists/{id}` | Altera nome e tipo, mesmo contrato da criação |
| `DELETE /shopping-lists/{id}` | Exclui lista e itens; retorna 204 |
| `POST /shopping-lists/{id}/items` | `{ "productId": "<uuid>", "quantity": 2 }`; retorna 201 |
| `PUT /shopping-lists/{id}/items/{itemId}` | `{ "quantity": 3 }` |
| `DELETE /shopping-lists/{id}/items/{itemId}` | Remove item; retorna 204 |
| `GET /comparisons/shopping-lists/{id}` | Exige `cityId` e autenticação do proprietário |

Consultas de catálogo e comparação de produto são públicas. Listas, comparação de listas e perfil exigem autenticação. Listas de outra pessoa retornam 404 em todas as operações. `shoppingType`: `DAILY`, `WEEKLY`, `MONTHLY` ou `CUSTOM`. Cada lista comporta até 200 produtos distintos; quantidades são positivas, até 999999, com no máximo três casas decimais. Produto repetido retorna 409; use a alteração de quantidade.

Coleções usam `page` (a partir de 0) e `size` (1–100, padrão 20), com ordenação determinística definida pelo servidor. Envelope: `content`, `page`, `size`, `totalElements`, `totalPages`. Nas comparações, esse envelope aparece em `stores` e pagina as lojas, não os itens da lista. Busca textual ignora capitalização; `%` e `_` são caracteres literais, não curingas. Não há matching automático por semelhança de nomes.

OpenAPI completo: `GET /v3/api-docs`, quando habilitado. A configuração descreve o Bearer nas rotas privadas. Os erros seguem `application/problem+json`: `type`, `title`, `status`, `detail`, `instance`, `code`, `path`, `timestamp`; erros de validação incluem `errors` com campo e mensagem, sem valor sensível. Principais status: 400, 401, 403, 404, 409, 422, 500 e 503.

## Semântica de comparação

- A última observação de cada produto/loja é escolhida por `collectedAt`, `recordedAt` e UUID, nessa ordem decrescente. A chegada de uma observação antiga não substitui uma coleta mais recente.
- `price.status` distingue `KNOWN`, `NO_OBSERVATION`, `EXPIRED` e `OUT_OF_STOCK`. `availability` distingue `UNKNOWN`, `AVAILABLE` e `UNAVAILABLE`; preço conhecido não garante estoque.
- Uma observação vence no menor limite entre `validUntil` e `collectedAt + PRICE_MAX_AGE`. Não se retorna um preço antigo para ocultar a expiração do mais recente.
- Promoção só entra no cálculo quando existe preço promocional menor que o regular e `promotionValidUntil` ainda válido. Sem validade promocional conhecida, usa-se o preço regular observado.
- `unitPrice` e `lineTotal` são `null` quando não há preço utilizável. `subtotalKnown` é `null` quando nenhum item tem preço. Nunca se substitui ausência por zero.
- `requestedItems`, `pricedItems` e `missingItems` contam linhas de produtos distintos. A quantidade de cada linha participa do cálculo. Cada subtotal de linha é arredondado para centavos com `HALF_UP`, depois somado com `BigDecimal`.
- `completeShoppingList` só é verdadeiro quando uma lista não vazia tem preço utilizável para todos os itens. Não significa estoque confirmado. Lista vazia tem total nulo e `completeShoppingList=false`.
- As lojas são ordenadas por nome/ID. A API não elege uma vencedora global a partir de uma página nem classifica um subtotal incompleto como compra inteira mais barata. Metadados da observação acompanham cada preço.

## Testes

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Testes de integração usam **PostgreSQL real**, as mesmas migrations e validação Hibernate. Se `initdb` e `pg_ctl` estiverem em `PATH`, o suporte cria um cluster temporário em `target/postgres-tests`, com senha aleatória, porta livre e acesso somente por loopback, encerrado ao finalizar a JVM. No Windows também detecta instalações em `Program Files/PostgreSQL`. Para indicar os binários, exporte `PG_BIN` com o caminho da pasta `bin`.

Outra opção é fornecer `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` e `TEST_DATABASE_PASSWORD` para um PostgreSQL dedicado a testes. O usuário precisa criar schemas: cada execução cria um schema exclusivo e o remove ao terminar. Nunca aponte essas variáveis para produção. Em Linux, execute os testes com usuário comum, pois `initdb` não roda como root. Se PostgreSQL não estiver disponível, o build falha explicitamente; não há testes automaticamente ignorados.

Os testes cobrem credenciais, tokens, ownership, CRUD de listas, validações, busca, normalização, integridade da ingestão, histórico, promoções, ausência de preços, precisão monetária, migrations e contratos HTTP/OpenAPI.

Se o Maven no Windows apresentar `PKIX path building failed` e o certificado necessário já for confiável no Windows, use o armazenamento de certificados do sistema somente nessa sessão, sem desativar TLS:

```powershell
$env:MAVEN_OPTS = "$env:MAVEN_OPTS -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE"
```

Caso contrário, configure no JDK o certificado confiável fornecido pela sua infraestrutura. `DEBUG=false` evita que uma variável global `DEBUG` habilite logs detalhados do Spring durante a execução.

## Arquitetura e fontes

Os pacotes `auth`, `user`, `location`, `store`, `product`, `datasource`, `price`, `shoppinglist`, `comparison` e `common` separam responsabilidades por domínio. Controllers validam contratos e delegam; serviços orquestram transações; repositories cuidam da persistência. Consultas de comparação buscam o último preço em lote para os produtos da lista e as lojas da página.

Contratos `ProductDataProvider`, `StoreDataProvider` e `PriceDataProvider` isolam integrações. Serviços internos de ingestão validam observações e preservam origem. Não existem endpoints públicos de escrita de catálogo nem endpoints administrativos. O registro de uma fonte exige verificação externa: persistir metadados não comprova sua autenticidade automaticamente.

Veja [integração de fontes](docs/data-sources.md) para os contratos, idempotência e requisitos de adapters. Permanecem fora desta etapa: coletor comercial homologado, busca por distância, matching aproximado, inteligência artificial, pagamentos e recursos avançados de segurança.

Referências técnicas verificadas: [requisitos do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [PasswordEncoder do Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/password-encoder.html), [springdoc sem interface web](https://springdoc.org/#_spring_webmvc_support) e [initdb do PostgreSQL](https://www.postgresql.org/docs/current/app-initdb.html).
"# SupermarketPrices" 
