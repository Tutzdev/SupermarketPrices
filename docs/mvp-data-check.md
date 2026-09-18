# Validação do MVP com dados reais

## Resultado executado em 17/09/2026

Ambiente Windows, PostgreSQL 18 nativo, Maven Wrapper, Java 26 compilando para Java 21 e frontend Vite. Docker não estava instalado; Docker Compose **não executado**. Banco isolado `gomo_acceptance` na porta 55433, backend 8081, frontend 5173. Os dados comerciais vieram exclusivamente das duas coletas HTTP ao vivo; fixtures ficaram nos testes.

A causa reproduzida das telas quebradas foi a URL padrão `http://localhost:8080/api/v1`: nessa máquina a porta 8080 pertencia ao **Apache/httpd**, que respondeu **404 HTML** para `/api/v1/products`, não à API Gomo. Com PostgreSQL e backend corretos em 8081, CORS para `http://localhost:5173` e `frontend/.env.local` apontando para `/api/v1`, os endpoints responderam JSON 200. Não foi constatado um erro 500 de domínio nesse fluxo. A comparação também ocultava falhas de carregamento de produtos/cidades como seletor vazio; agora exibe erro e permite tentar novamente. O contrato paginado é `{ content, totalElements, totalPages, ... }`, sem envelope `data`.

Flyway aplicou **12 migrations** em banco limpo. A única migration nova é `V12__record_collection_availability_counts.sql`, que acrescenta contadores às execuções; nenhuma migration anterior foi alterada e nenhum seed comercial foi criado.

| Coleta | Fonte | Início → fim (America/Sao_Paulo) | Recebidos | Produtos criados | Cadastros atualizados | Observações inseridas | Descartes com erro | Status |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| 1 | Nagumo Ponte Alta | 18:25:49 → 18:26:21 | 472 | 462 | 0 | 462 | 10 | PARTIAL |
| 1 | Royal Retiro | 18:26:23 → 18:26:58 | 225 | 205 | 0 | 225 | 0 | SUCCESS |
| 2 | Nagumo Ponte Alta | 18:27:21 → 18:27:51 | 472 | 0 | 462 | 462 | 10 | PARTIAL |
| 2 | Royal Retiro | 18:27:53 → 18:28:24 | 225 | 0 | 225 | 225 | 0 | SUCCESS |

Execuções: `fba9a271-4fe9-464e-a817-d503e29626e9`, `3a5a35ac-e839-4947-b4b3-06f6c41cd5de`, `24717e01-26f8-4d41-9dda-22a5d648832d`, `f969f4da-55c3-4305-a54c-3640dd004d1b`.

SQL após duas coletas: **2 lojas, 667 produtos canônicos, 687 vínculos de produto/fonte, 1.374 observações** (924 Nagumo, 450 Royal), **0 referências duplicadas**. São 20 canônicos compartilhados, 14 com preço utilizável nas duas lojas; os outros seis têm indisponibilidade em pelo menos uma fonte. Em cada coleta os válidos eram Nagumo 373 disponíveis/89 indisponíveis, Royal 179/46, nenhum UNKNOWN. Os dez descartes Nagumo não entram nesses contadores.

`collectedAt` da segunda coleta: Nagumo **2026-09-17T21:27:50.392274Z**, Royal **2026-09-17T21:28:24.129136Z**. Novas observações temporais são esperadas, mesmo com preço igual. O replay do mesmo catálogo/instante foi validado no teste integrado: não acrescenta observações. Também foi reenviada duas vezes pela API administrativa a observação real `62f63069-61e8-460b-b66d-8a32c13e4b35`, sem alterar seu conteúdo: ambas retornaram o mesmo ID, e o histórico continuou com dois registros. A unique constraint `(source_id, source_reference)` permanece ativa.

| Produto revisado | Nagumo — preço regular | Royal Retiro — preço regular |
| --- | ---: | ---: |
| Coca-Cola Original 2 L | R$ 11,98 | R$ 12,79 |
| Lentilha Yoki 400 g | R$ 16,49 | R$ 19,99 |
| Bisnaguinha Bauducco 260 g | R$ 8,69 | R$ 8,99 |
| Cappuccino Classic 3 Corações 200 g | R$ 19,90 | R$ 21,90 |
| Café Solúvel Tradicional 3 Corações 40 g | R$ 7,65 | R$ 8,09 |

Exemplo de promoção preservada: Coca-Cola Original 2 L no Royal tinha preço promocional observado de **R$ 11,99**, sem validade explícita; pela política existente o comparador usa o regular e mostra a promoção separadamente. Preços acima são evidência daquele instante, não promessa de preço atual.

Identidades: Nagumo loja **36 / 036-V.REDONDA**, Ponte Alta, Via Sérgio Braga, 951; Royal **organização 255 / filial 2 / CD 1 / VipCommerce 959**, CNPJ **39553144000100**, Avenida Antônio de Almeida, 1477, Retiro. O número 951 não consta do JSON Nagumo; a fonte confirma ID, nome, via, bairro e cidade. Protocolos, domínios e limitações em [data-sources.md](data-sources.md).

## Reproduzir localmente no Windows

Pré-requisitos usados: PostgreSQL 18 em `C:\Program Files\PostgreSQL\18\bin`, Java compatível com release 21, Node/npm e Python **3.11** para o SMTP de desenvolvimento. Execute os blocos a partir da raiz do repositório. O SMTP local apenas recebe a confirmação de conta; nunca fornece dados comerciais. Python 3.12+ não inclui `smtpd`; nesse caso use Mailpit e configure seu host/porta SMTP.

### 1. Criar banco limpo isolado

O bloco cria outro diretório; não apaga seu banco atual. Se `.local/gomo-check` já existir, escolha um diretório novo e uma porta livre.

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\18\bin'
$checkDir = Join-Path $PWD '.local/gomo-check'
if (Test-Path -LiteralPath $checkDir) { throw 'Diretório já existe; escolha outro para validar do zero.' }
New-Item -ItemType Directory -Path $checkDir | Out-Null
$databasePassword = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
[IO.File]::WriteAllText((Join-Path $checkDir 'pg-password'), $databasePassword)
& "$pgBin\initdb.exe" -D "$checkDir\pgdata" -U gomo_mvp --encoding=UTF8 --auth=scram-sha-256 --pwfile="$checkDir\pg-password"
& "$pgBin\pg_ctl.exe" -D "$checkDir\pgdata" -l "$checkDir\postgres.log" -o '-p 55434 -h 127.0.0.1' start
$env:PGPASSWORD = $databasePassword
& "$pgBin\createdb.exe" -h 127.0.0.1 -p 55434 -U gomo_mvp gomo_acceptance
```

A execução registrada neste documento usou 55433; o roteiro usa 55434 para não interrompê-la. `.local/` é ignorado pelo Git; o arquivo de senha é apenas local e deve ficar protegido pela conta do sistema operacional.

### 2. SMTP e backend

Em um terminal separado, com a porta 1025 livre:

```powershell
python -m smtpd -n -c DebuggingServer 127.0.0.1:1025
```

Em outro terminal, na raiz:

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55434/gomo_acceptance'
$env:DATABASE_USERNAME = 'gomo_mvp'
$env:DATABASE_PASSWORD = [IO.File]::ReadAllText((Join-Path $PWD '.local/gomo-check/pg-password'))
$env:PORT = '8081'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173'
$env:MAIL_ENABLED = 'true'
$env:SPRING_MAIL_HOST = '127.0.0.1'
$env:SPRING_MAIL_PORT = '1025'
$env:ADMIN_BOOTSTRAP_EMAIL = ''
$env:SUBSCRIBER_BOOTSTRAP_EMAIL = ''
.\mvnw.cmd spring-boot:run
```

Aguarde `Tomcat started on port 8081` e `Started PricesApplication`. As configurações completas estão em `.env.example`. O Maven/Spring não carrega automaticamente esse arquivo; exporte as variáveis acima ou use o Compose, que já encaminha as flags dos coletores. Não aponte frontend para 8080 se outro serviço estiver nessa porta.

### 3. Criar, confirmar e provisionar a conta

Em um terminal separado:

```powershell
$api = 'http://localhost:8081/api/v1'
$email = Read-Host 'E-mail da sua conta local'
$securePassword = Read-Host 'Senha de 12 a 72 caracteres' -AsSecureString
$password = [Net.NetworkCredential]::new('', $securePassword).Password
$registration = @{name='Gomo local';email=$email;password=$password} | ConvertTo-Json
Invoke-RestMethod "$api/auth/register" -Method Post -ContentType application/json -Body ([Text.Encoding]::UTF8.GetBytes($registration))
$verificationToken = Read-Host 'Token recebido no terminal SMTP'
Invoke-RestMethod "$api/auth/email-verifications/confirm" -Method Post -ContentType application/json -Body (@{token=$verificationToken} | ConvertTo-Json)
```

Interrompa somente o backend com Ctrl+C e reinicie **no mesmo terminal das variáveis do backend**, configurando o e-mail confirmado:

```powershell
$env:ADMIN_BOOTSTRAP_EMAIL = Read-Host 'O mesmo e-mail confirmado'
$env:SUBSCRIBER_BOOTSTRAP_EMAIL = $env:ADMIN_BOOTSTRAP_EMAIL
.\mvnw.cmd spring-boot:run
```

Esse é o provisionamento oficial existente. Não remove autorização nem simula pagamento. Cadastro, confirmação e login foram executados na validação; o bootstrap exige e-mail verificado. Remova as duas variáveis de bootstrap depois do primeiro provisionamento, quando reiniciar novamente.

No terminal da conta, faça login sem imprimir o token:

```powershell
$login = Invoke-RestMethod "$api/auth/login" -Method Post -ContentType application/json -Body (@{email=$email;password=$password} | ConvertTo-Json)
$headers = @{Authorization="Bearer $($login.accessToken)"}
```

### 4. Coletar, consultar e comparar

```powershell
$runs = Invoke-RestMethod "$api/admin/collections" -Method Post -Headers $headers -TimeoutSec 300
$runs | Select-Object id,collectorCode,status,foundCount,createdCount,productsUpdatedCount,observationsInsertedCount,observationsSkippedCount,errorCount
Invoke-RestMethod "$api/admin/collections/$($runs[0].id)" -Headers $headers
Invoke-RestMethod "$api/admin/collections?size=20" -Headers $headers
$cities = Invoke-RestMethod "$api/cities?size=100"
$city = $cities.content | Where-Object name -eq 'Volta Redonda'
$stores = Invoke-RestMethod "$api/stores?cityId=$($city.id)&size=100"
$stores.content | Select-Object id,name,address,lastPriceCollectedAt
$products = Invoke-RestMethod "$api/products?query=7894900027013&size=20"
$products.content | Select-Object id,name,gtin
$product = $products.content | Select-Object -First 1
Invoke-RestMethod "$api/prices?productId=$($product.id)&storeId=$($stores.content[0].id)"
$comparison = Invoke-RestMethod "$api/comparisons/products?productId=$($product.id)&cityId=$($city.id)"
$comparison.stores.content | Select-Object storeName,price
Invoke-RestMethod 'http://localhost:8081/v3/api-docs' | Select-Object openapi
```

O POST é síncrono e pode levar cerca de um minuto. Verifique `status` e `errorMessage`, não apenas HTTP 200. Repita o POST para obter nova observação temporal; produtos e vínculos devem continuar estáveis. O agendamento global segue habilitado, `0 30 5 * * *`, `America/Sao_Paulo`, Nagumo seguido de Royal. Não foi necessário esperar até 05:30 para validar: o endpoint executa o mesmo coordenador, e o isolamento de falhas tem teste automatizado.

### 5. Frontend

```powershell
[IO.File]::WriteAllText((Join-Path $PWD 'frontend/.env.local'), "VITE_API_BASE_URL=http://localhost:8081/api/v1`n")
Set-Location frontend
npm install
npm run dev -- --host localhost
```

Abra <http://localhost:5173/entrar>, use a conta confirmada e visite `/app/produtos`, `/app/supermercados` e `/app/comparar`. Escolha Volta Redonda, pesquise `7894900027013`, selecione Coca-Cola Original 2 L e compare. Os IDs são gerados pelo seu banco; nunca copie UUIDs do relatório para outra instalação.

### 6. Conferir integridade e executar os checks

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\18\bin'
$env:PGPASSWORD = [IO.File]::ReadAllText((Join-Path $PWD '.local/gomo-check/pg-password'))
& "$pgBin\psql.exe" -h 127.0.0.1 -p 55434 -U gomo_mvp -d gomo_acceptance -c 'select count(*) from products; select count(*) from price_records;'
& "$pgBin\psql.exe" -h 127.0.0.1 -p 55434 -U gomo_mvp -d gomo_acceptance -c 'select source_id,source_reference,count(*) from price_records group by source_id,source_reference having count(*)>1;'
& "$pgBin\psql.exe" -h 127.0.0.1 -p 55434 -U gomo_mvp -d gomo_acceptance -c 'select product_id,count(distinct source_id) from product_source_references group by product_id having count(distinct source_id)=2;'
.\mvnw.cmd test
.\mvnw.cmd verify
Set-Location frontend
npm run build
npm run lint
```

Execute o último bloco a partir da raiz. Os testes usam a infraestrutura PostgreSQL isolada já existente em `PostgresTestDatabase`; não dependem dos sites externos. Fixtures e sanitização: [README dos snapshots](../src/test/resources/fixtures/collectors/README.md).

## Verificações e limites

- `mvnw.cmd test`: **168 testes, zero falhas/erros/skips**.
- `mvnw.cmd verify`: **168 testes, zero falhas/erros/skips**, JAR gerado.
- `npm install`, `npm run build` e `npm run lint`: executados com sucesso. `npm test`: **não executado**, não existe script/suíte configurada.
- APIs reais de cidades, lojas, produtos, histórico, comparação e coleções verificadas, com os seis caminhos também conferidos no OpenAPI. Busca por marca/nome, GTIN e termo inexistente respondendo sem falsos vazios de erro.
- Login real no navegador; Produtos, Supermercados e Comparar usando o banco acima. Verificados Coca-Cola nas duas lojas, creme de leite Italac indisponível no Royal, papel Nagumo com condição Meu Nagumo e Royal sem observação. Capturas locais em `.local/mvp/`; arquivos de sessão do navegador são ignorados pelo Git.
- Falha de rede controlada no navegador: a requisição de produtos foi interrompida somente durante o teste; a tela mostrou erro e recuperou os dados reais ao clicar em “Tentar novamente”. Nenhuma resposta comercial foi simulada. Layout conferido em desktop e celular.
- Cobertura de catálogo é deliberadamente limitada às categorias/buscas documentadas. Dez itens Nagumo sem preço não foram ingeridos. Estoque indisponível não vira preço zero.
- Bundles e contratos externos podem mudar; mudanças de filial ou vínculo revisado interrompem a ingestão correspondente. Os vínculos são conservadores e precisam de revisão quando embalagem/identidade mudar.
- Não houve cobrança real; o acesso de assinante usou o bootstrap administrativo existente. O cron permaneceu configurado, mas não foi aguardado o disparo do dia seguinte.
- O compilador executado com Java 26 emitiu avisos de dependências sobre `Unsafe`/acesso nativo; não impediram os testes ou o pacote. Nenhum aviso foi suprimido para forçar sucesso.

## Arquivos desta tarefa

As modificações preexistentes em `dashboard-sidebar.tsx`, `native-button.tsx`, `index.css` e `admin-page.tsx` foram preservadas. Relação dos arquivos alterados ou acrescentados nesta tarefa:

- [.env.example](../.env.example)
- [.gitignore](../.gitignore)
- [README.md](../README.md)
- [compose.yml](../compose.yml)
- [docs/data-sources.md](../docs/data-sources.md)
- [docs/mvp-data-check.md](../docs/mvp-data-check.md)
- [frontend/src/lib/api.ts](../frontend/src/lib/api.ts)
- [frontend/src/routes/app/comparison-page.tsx](../frontend/src/routes/app/comparison-page.tsx)
- [frontend/src/routes/app/products-page.tsx](../frontend/src/routes/app/products-page.tsx)
- [frontend/src/routes/app/stores-page.tsx](../frontend/src/routes/app/stores-page.tsx)
- [frontend/src/routes/landing-page.tsx](../frontend/src/routes/landing-page.tsx)
- [frontend/src/types/api.ts](../frontend/src/types/api.ts)
- [src/main/java/br/com/supermercados/prices/collection/CollectedCatalogIngestionService.java](../src/main/java/br/com/supermercados/prices/collection/CollectedCatalogIngestionService.java)
- [src/main/java/br/com/supermercados/prices/collection/CollectionConfiguration.java](../src/main/java/br/com/supermercados/prices/collection/CollectionConfiguration.java)
- [src/main/java/br/com/supermercados/prices/collection/CollectionResult.java](../src/main/java/br/com/supermercados/prices/collection/CollectionResult.java)
- [src/main/java/br/com/supermercados/prices/collection/CollectionRun.java](../src/main/java/br/com/supermercados/prices/collection/CollectionRun.java)
- [src/main/java/br/com/supermercados/prices/collection/CollectionRunResponse.java](../src/main/java/br/com/supermercados/prices/collection/CollectionRunResponse.java)
- [src/main/java/br/com/supermercados/prices/collection/PriceObservationReference.java](../src/main/java/br/com/supermercados/prices/collection/PriceObservationReference.java)
- [src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoClient.java](../src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoClient.java)
- [src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoCollector.java](../src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoCollector.java)
- [src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoProductParser.java](../src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoProductParser.java)
- [src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoProperties.java](../src/main/java/br/com/supermercados/prices/collection/nagumo/NagumoProperties.java)
- [src/main/java/br/com/supermercados/prices/collection/royal/RoyalClient.java](../src/main/java/br/com/supermercados/prices/collection/royal/RoyalClient.java)
- [src/main/java/br/com/supermercados/prices/collection/royal/RoyalCollector.java](../src/main/java/br/com/supermercados/prices/collection/royal/RoyalCollector.java)
- [src/main/java/br/com/supermercados/prices/collection/royal/RoyalProductParser.java](../src/main/java/br/com/supermercados/prices/collection/royal/RoyalProductParser.java)
- [src/main/java/br/com/supermercados/prices/collection/royal/RoyalProperties.java](../src/main/java/br/com/supermercados/prices/collection/royal/RoyalProperties.java)
- [src/main/java/br/com/supermercados/prices/price/PriceRecordRepository.java](../src/main/java/br/com/supermercados/prices/price/PriceRecordRepository.java)
- [src/main/java/br/com/supermercados/prices/price/StorePriceUpdate.java](../src/main/java/br/com/supermercados/prices/price/StorePriceUpdate.java)
- [src/main/java/br/com/supermercados/prices/product/ProductIngestionService.java](../src/main/java/br/com/supermercados/prices/product/ProductIngestionService.java)
- [src/main/java/br/com/supermercados/prices/product/ProductSearch.java](../src/main/java/br/com/supermercados/prices/product/ProductSearch.java)
- [src/main/java/br/com/supermercados/prices/product/VerifiedProductMappings.java](../src/main/java/br/com/supermercados/prices/product/VerifiedProductMappings.java)
- [src/main/java/br/com/supermercados/prices/store/StoreResponse.java](../src/main/java/br/com/supermercados/prices/store/StoreResponse.java)
- [src/main/java/br/com/supermercados/prices/store/StoreService.java](../src/main/java/br/com/supermercados/prices/store/StoreService.java)
- [src/main/resources/application.properties](../src/main/resources/application.properties)
- [src/main/resources/db/migration/V12__record_collection_availability_counts.sql](../src/main/resources/db/migration/V12__record_collection_availability_counts.sql)
- [src/main/resources/product-mappings/nagumo-royal.json](../src/main/resources/product-mappings/nagumo-royal.json)
- [src/test/java/br/com/supermercados/prices/PublicCollectionIntegrationTests.java](../src/test/java/br/com/supermercados/prices/PublicCollectionIntegrationTests.java)
- [src/test/java/br/com/supermercados/prices/collection/nagumo/NagumoCollectorTest.java](../src/test/java/br/com/supermercados/prices/collection/nagumo/NagumoCollectorTest.java)
- [src/test/java/br/com/supermercados/prices/collection/nagumo/NagumoPublicResponsesTest.java](../src/test/java/br/com/supermercados/prices/collection/nagumo/NagumoPublicResponsesTest.java)
- [src/test/java/br/com/supermercados/prices/collection/royal/RoyalCollectorTest.java](../src/test/java/br/com/supermercados/prices/collection/royal/RoyalCollectorTest.java)
- [src/test/java/br/com/supermercados/prices/comparison/ShoppingComparisonServiceTest.java](../src/test/java/br/com/supermercados/prices/comparison/ShoppingComparisonServiceTest.java)
- [src/test/java/br/com/supermercados/prices/product/ProductIngestionServiceTest.java](../src/test/java/br/com/supermercados/prices/product/ProductIngestionServiceTest.java)
- [src/test/java/br/com/supermercados/prices/product/VerifiedProductMappingsTest.java](../src/test/java/br/com/supermercados/prices/product/VerifiedProductMappingsTest.java)
- [src/test/java/br/com/supermercados/prices/support/CollectorFixtureServer.java](../src/test/java/br/com/supermercados/prices/support/CollectorFixtureServer.java)
- [src/test/resources/application-test.properties](../src/test/resources/application-test.properties)
- [src/test/resources/fixtures/collectors/README.md](../src/test/resources/fixtures/collectors/README.md)
- [src/test/resources/fixtures/collectors/nagumo-MP-GERAL-0.json](../src/test/resources/fixtures/collectors/nagumo-MP-GERAL-0.json)
- [src/test/resources/fixtures/collectors/nagumo-MP-GERAL-100.json](../src/test/resources/fixtures/collectors/nagumo-MP-GERAL-100.json)
- [src/test/resources/fixtures/collectors/nagumo-MP-GERAL-50.json](../src/test/resources/fixtures/collectors/nagumo-MP-GERAL-50.json)
- [src/test/resources/fixtures/collectors/nagumo-PARCEIRO-COCA-COLA-0.json](../src/test/resources/fixtures/collectors/nagumo-PARCEIRO-COCA-COLA-0.json)
- [src/test/resources/fixtures/collectors/nagumo-empty.json](../src/test/resources/fixtures/collectors/nagumo-empty.json)
- [src/test/resources/fixtures/collectors/nagumo-stores.json](../src/test/resources/fixtures/collectors/nagumo-stores.json)
- [src/test/resources/fixtures/collectors/royal-coca-1.json](../src/test/resources/fixtures/collectors/royal-coca-1.json)
- [src/test/resources/fixtures/collectors/royal-coca-2.json](../src/test/resources/fixtures/collectors/royal-coca-2.json)
- [src/test/resources/fixtures/collectors/royal-domain.json](../src/test/resources/fixtures/collectors/royal-domain.json)
- [src/test/resources/fixtures/collectors/royal-empty.json](../src/test/resources/fixtures/collectors/royal-empty.json)
- [src/test/resources/fixtures/collectors/royal-pickups.json](../src/test/resources/fixtures/collectors/royal-pickups.json)
- [src/test/resources/fixtures/collectors/royal-store.json](../src/test/resources/fixtures/collectors/royal-store.json)
- [src/test/resources/fixtures/collectors/royal-yoki-1.json](../src/test/resources/fixtures/collectors/royal-yoki-1.json)
