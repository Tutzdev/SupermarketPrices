# Supermarket Prices API

Backend REST para comparação de preços de supermercados por cidade e por lista de compras. Além do catálogo e das comparações, a API oferece administração auditada, contribuições moderadas, recomendação da compra completa, preferências, alertas internos e recuperação de conta.

O escopo inicial de localidades é Rio de Janeiro/RJ, Volta Redonda, Barra Mansa e Resende. Essas localidades são dados de uma migration, não condições fixas na regra de negócio.

Stack: Java 21, Spring Boot 4.1.1, Maven Wrapper, Spring MVC, Data JPA, Security, Bean Validation, Actuator, PostgreSQL, Flyway e OpenAPI. A autenticação usa tokens opacos persistidos somente por hash; não há frontend neste repositório.

## Estado dos dados

Nenhuma loja, rede, fonte, produto ou preço comercial é pré-cadastrado. Não há coletor externo ativo nem fonte comercial homologada. Fixtures fictícias ficam exclusivamente em `src/test/` e nunca entram no JAR ou nas migrations de produção.

Somente observações inseridas por uma fonte administrativa habilitada ou originadas de contribuições aprovadas participam das comparações. Contribuições pendentes e rejeitadas permanecem no histórico de moderação e não publicam preços.

## Execução local

Requisitos para execução sem Docker: JDK 21 e PostgreSQL 18. O Maven Wrapper dispensa instalação global do Maven.

### Aplicação, PostgreSQL e e-mail local com Docker

1. Copie `.env.example` para `.env`.
2. Defina `DATABASE_USERNAME` e uma senha não vazia em `DATABASE_PASSWORD`.
3. Execute:

```powershell
docker compose --env-file .env up --build -d
```

A API fica em `http://localhost:8080`, o PostgreSQL em `localhost:5432` e o Mailpit em `http://localhost:8025`. As três portas são vinculadas apenas ao loopback. O Mailpit captura localmente as mensagens de verificação e recuperação; isso não representa entrega real de e-mail.

Para encerrar os containers sem remover o volume do banco:

```powershell
docker compose down
```

### Aplicação pelo Maven

Inicie um PostgreSQL, exporte as variáveis no processo e execute:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'development'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/prices'
$env:DATABASE_USERNAME = 'prices'
$env:DATABASE_PASSWORD = '<senha>'
$env:MAIL_ENABLED = 'false'
.\mvnw.cmd spring-boot:run
```

Spring Boot e Maven não carregam `.env` automaticamente. Para empacotar, execute `.\mvnw.cmd clean verify` e inicie `java -jar target/prices-0.0.1-SNAPSHOT.jar` com as variáveis exportadas.

## Configuração

| Variável | Uso / padrão |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `development` ou `production`; não há perfil implícito |
| `DATABASE_URL` | URL JDBC PostgreSQL; obrigatória fora de development |
| `DATABASE_USERNAME` | Usuário do banco; obrigatório fora de development |
| `DATABASE_PASSWORD` | Senha do banco, sempre externa |
| `DATABASE_POOL_SIZE` | Máximo de conexões; padrão 10 |
| `PORT` | Porta HTTP; padrão 8080 |
| `CORS_ALLOWED_ORIGINS` | Origens HTTP(S) exatas separadas por vírgula; vazio bloqueia chamadas entre origens |
| `AUTH_TOKEN_TTL` | Duração ISO-8601 da sessão; padrão `PT12H` |
| `EMAIL_VERIFICATION_TTL` | Validade da verificação; padrão `P1D` |
| `PASSWORD_RESET_TTL` | Validade da recuperação; padrão `PT1H` |
| `PRICE_MAX_AGE` | Idade máxima de uma observação; padrão `P2D` |
| `RECOMMENDATION_MAX_STORES` | Limite de lojas para recomendação completa; padrão 500, máximo 5000 |
| `ALERT_REPEAT_INTERVAL` | Intervalo mínimo entre notificações do mesmo alerta; padrão `P1D` |
| `CONTRIBUTION_SOURCE_ID` | UUID da fonte habilitada usada para publicar contribuições aprovadas |
| `ADMIN_BOOTSTRAP_EMAIL` | E-mail verificado que pode ser promovido quando ainda não existe administrador |
| `MAIL_ENABLED` | Habilita SMTP; padrão `false` fora do Compose |
| `MAIL_FROM` | Remetente das mensagens; padrão `no-reply@localhost` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | Servidor SMTP; o Compose usa Mailpit em `mailpit:1025` |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Credenciais SMTP, quando exigidas pelo provedor |
| `REGISTRATION_RATE_LIMIT` | Cadastros por IP por hora; padrão 5 |
| `LOGIN_RATE_LIMIT` | Tentativas por IP e por e-mail por minuto; padrão 10 |
| `ACCOUNT_MESSAGE_RATE_LIMIT` | Pedidos de recuperação por IP/e-mail por hora; padrão 5 |
| `CONTRIBUTION_RATE_LIMIT` | Contribuições por usuário por hora; padrão 20 |
| `OPENAPI_ENABLED` | Expõe `/v3/api-docs`; `true` em development e `false` em production |

Senhas, tokens e credenciais não têm valores de produção versionados. Configure TLS e SMTP de acordo com a infraestrutura. Quando `MAIL_ENABLED=false`, o backend gera e persiste os tokens, mas apenas registra que o envio está desabilitado, sem escrever o token em log.

## Primeiro administrador

Não existe usuário ou senha administrativa padrão. O provisionamento inicial exige uma conta real e e-mail confirmado:

1. Cadastre o usuário por `POST /api/v1/auth/register`.
2. Abra a mensagem no Mailpit ou no SMTP configurado e confirme o token.
3. Defina `ADMIN_BOOTSTRAP_EMAIL` com o e-mail dessa conta e reinicie a aplicação.
4. Confirme que a conta recebeu o papel `ADMIN` e remova a variável.

O provisionador só atua quando ainda não existe administrador. Cadastro público sempre cria `USER` e nunca aceita papel no corpo da requisição.

Depois, o administrador deve cadastrar uma fonte verificável em `POST /api/v1/admin/sources`. Para contribuições, cadastre uma fonte que represente esta própria plataforma, com a URL real do deployment, e configure seu UUID em `CONTRIBUTION_SOURCE_ID`. A aprovação retorna 503 enquanto essa configuração estiver ausente ou desabilitada; nenhuma fonte fictícia é criada automaticamente.

## Conta e segurança

- `POST /api/v1/auth/register` cria uma conta `USER` e solicita verificação de e-mail.
- `POST /api/v1/auth/login` mantém compatibilidade e permite login ainda não verificado.
- `POST /api/v1/auth/email-verifications` solicita nova mensagem para o usuário autenticado.
- `POST /api/v1/auth/email-verifications/confirm` confirma um token.
- `POST /api/v1/auth/password-resets` sempre retorna 202, exista ou não o e-mail.
- `POST /api/v1/auth/password-resets/confirm` redefine a senha e revoga todas as sessões anteriores.
- `POST /api/v1/auth/logout` revoga a sessão apresentada.

Tokens de acesso, verificação e recuperação são aleatórios, armazenados por hash SHA-256, expiram e são de uso único quando aplicável. Senhas usam BCrypt com custo 12 e devem ter entre 12 caracteres e 72 bytes UTF-8. Enviar contribuições e criar alertas exige e-mail verificado; leitura do catálogo, login, listas e preferências mantêm a compatibilidade anterior.

A limitação de tentativas é uma proteção em memória, por janela fixa. Seus contadores são locais ao processo, desaparecem em reinícios e não são compartilhados entre réplicas. Uma implantação com múltiplas instâncias deve substituir ou complementar essa proteção por um limitador distribuído no gateway ou em armazenamento compartilhado.

## Endpoints principais

Todos os endpoints usam o prefixo `/api/v1`. IDs são UUIDs, dinheiro usa `BigDecimal`/BRL e timestamps representam instantes ISO-8601 em UTC. Coleções paginadas aceitam `page` e `size` de 1 a 100.

### Públicos

| Método e caminho | Finalidade |
| --- | --- |
| `GET /states`, `GET /cities` | Localidades cadastradas |
| `GET /chains`, `GET /stores` | Redes e lojas ativas |
| `GET /products` | Catálogo e busca de produtos |
| `GET /prices` | Histórico por produto e loja |
| `GET /comparisons/products` | Comparação de um produto na cidade |

### Usuário autenticado

| Método e caminho | Finalidade |
| --- | --- |
| `GET/PATCH /users/me` | Perfil da conta |
| `GET/POST/PUT/DELETE /shopping-lists/**` | Listas e itens isolados por proprietário |
| `GET /comparisons/shopping-lists/{id}` | Comparação paginada da lista |
| `GET /comparisons/shopping-lists/{id}/recommendation` | Recomendação global dentro do limite configurado |
| `GET/PUT/DELETE /users/me/preferences/**` | Cidade preferida e lojas favoritas |
| `POST/GET /contributions` | Envio e consulta das próprias contribuições |
| `POST/GET/PATCH /alerts/**` | Criação, consulta e desativação de alertas |
| `GET/PATCH /notifications/**` | Notificações internas e marcação de leitura |

Recursos de outro usuário retornam 404. O backend sempre deriva o proprietário do token; IDs ou papéis enviados pelo cliente não autorizam acesso.

### Administração

Todas as rotas `/api/v1/admin/**` exigem `ROLE_ADMIN` no backend.

| Método e caminho | Finalidade |
| --- | --- |
| `POST /admin/sources` | Cadastra fonte verificada |
| `PATCH /admin/sources/{id}` | Habilita ou desabilita fonte |
| `POST /admin/chains` | Ingere rede reutilizando o serviço existente |
| `POST /admin/stores` | Ingere loja |
| `POST /admin/products` | Ingere produto |
| `POST /admin/prices` | Registra observação imutável de preço |
| `GET /admin/contributions` | Fila filtrada por status |
| `POST /admin/contributions/{id}/approval` | Aprova e publica uma contribuição |
| `POST /admin/contributions/{id}/rejection` | Rejeita com motivo |
| `GET /admin/audit` | Consulta autor, ação, recurso e instante |

Aprovação e rejeição usam bloqueio pessimista e transação única. Repetir a mesma decisão é idempotente; uma decisão diferente retorna 409. A observação publicada referencia permanentemente a contribuição e tem origem `USER_CONTRIBUTION`.

## Comparação, recomendação e alertas

- A observação atual é escolhida por `collectedAt`, `recordedAt` e UUID, em ordem decrescente.
- A validade termina no menor valor entre `validUntil` e `collectedAt + PRICE_MAX_AGE`.
- Promoção só entra no cálculo quando é menor que o preço regular e sua validade conhecida ainda está ativa.
- `KNOWN`, `NO_OBSERVATION`, `EXPIRED` e `OUT_OF_STOCK` distinguem as condições do preço; estoque `UNKNOWN` permanece explícito.
- Totais de linha são arredondados para centavos com `HALF_UP` antes da soma.
- Uma loja só pode vencer a recomendação quando todos os itens possuem preço utilizável. Subtotais incompletos nunca são tratados como total completo.
- Se nenhuma loja for completa, o status é `NO_COMPLETE_STORE`, `recommendation` é nulo e até dez candidatos de maior cobertura são informados separadamente.
- Empates completos são resolvidos por total, nome da loja sem diferenciar maiúsculas e UUID.
- A recomendação consulta todas as lojas elegíveis em lote. Se a cidade superar `RECOMMENDATION_MAX_STORES`, retorna 422 em vez de declarar um vencedor parcial.

Alertas são avaliados ao persistir uma nova observação, inclusive a contribuição aprovada, reutilizando `PricePolicy`. O par alerta/observação é único no banco e o intervalo configurável evita disparos frequentes por observações diferentes. As notificações são internas e não enviam e-mail.

## Operação

Actuator expõe:

- `GET /actuator/health` e `/actuator/health/readiness` publicamente, sem detalhes internos;
- `/actuator`, `/actuator/info` e `/actuator/metrics/**` somente para administradores.

Logs de moderação identificam contribuição e UUID do moderador, sem registrar senha, token ou e-mail. A tabela de auditoria registra as alterações administrativas relevantes. O encerramento HTTP é gracioso.

OpenAPI completo: `GET /v3/api-docs`, quando habilitado. Erros usam `application/problem+json`; validações não reproduzem valores sensíveis.

## Banco e migrations

Flyway aplica migrations na inicialização e Hibernate apenas valida o schema (`ddl-auto=validate`):

- V1–V6: usuários, catálogo, localidades, preços, listas e controle de concorrência existente;
- V7: papéis, auditoria, contribuições moderadas e origem dos preços;
- V8: preferências, favoritos, alertas e notificações;
- V9: tokens de verificação e recuperação de conta.

Referências históricas usam `ON DELETE RESTRICT`; dados necessários para explicar preços anteriores não são apagados em cascata. Mudanças futuras devem criar novas migrations, sem editar as já aplicadas.

## Testes e CI

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Os testes de integração usam PostgreSQL real, as mesmas migrations e validação Hibernate. Se `initdb` e `pg_ctl` estiverem disponíveis, o suporte cria um cluster temporário em `target/postgres-tests`. Alternativamente, configure `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` e `TEST_DATABASE_PASSWORD` para um banco exclusivo de testes; cada execução cria e remove seu próprio schema.

A suíte cobre autenticação e autorização administrativa, isolamento por proprietário, validade e promoções, recomendação completa além da primeira página, contribuições rejeitadas, aprovação idempotente e concorrente, alertas sem duplicação, tokens expirados/reutilizados, revogação de sessões e limitação de tentativas. O workflow em `.github/workflows/ci.yml` executa `clean verify` com Java 21 e PostgreSQL 18.

## Arquitetura e integrações pendentes

Os pacotes são organizados por funcionalidade. Controllers tratam HTTP, serviços coordenam regras e transações, repositories cuidam de persistência e DTOs impedem exposição das entidades JPA. Ingestão administrativa reutiliza os serviços existentes de catálogo e preço.

Veja [integração de fontes](docs/data-sources.md) para contratos, idempotência e requisitos de adapters. Continuam fora do escopo: fonte comercial homologada, scraping sem autorização, busca por distância, matching aproximado, cobrança, painel de lojistas, frontend e limitador distribuído. SMTP real também depende das credenciais externas do ambiente; apenas o fluxo local com Mailpit está configurado no Compose.

## Autor

Desenvolvido por [Tutzdev](https://github.com/Tutzdev).
