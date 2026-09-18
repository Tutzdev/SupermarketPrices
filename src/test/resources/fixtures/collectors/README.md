# Respostas públicas dos coletores

Capturadas em **17/09/2026**, das requisições usadas pelos próprios e-commerces. Não são usadas pelo runtime nem apresentadas como coleta real na interface.

- `nagumo-stores.json`: `https://www.nagumo.com.br/on/demandware.store/Sites-Nagumo-Site/pt_BR/Stores-AvailableStores`; preservada somente a loja 36, removido UUID da sessão.
- `nagumo-MP-GERAL-{0,50,100}.json` e `nagumo-PARCEIRO-COCA-COLA-0.json`: `Search-UpdateGrid?cgid=<categoria>&start=<posição>&sz=50`, após selecionar loja 36 por `Stores-SelectStore`. Preservados os campos consumidos pelo parser, inclusive os preços originais, disponibilidade e bandeiras de promoção.
- `royal-domain.json`: `https://services.vipcommerce.com.br/api-admin/v1/organizacoes/filiais/dominio/royalsupermercados.com.br`.
- `royal-pickups.json`: `/api-admin/v1/org/255/filial/2/loja/centros_distribuicoes/retiradas`.
- `royal-store.json`: `/api-admin/v1/org/255/loja/centros_distribuicoes/1`, Retiro, CNPJ 39553144000100.
- `royal-{coca,yoki}-<página>.json`: `/api-admin/v1/org/255/filial/2/centro_distribuicao/1/loja/buscas/produtos/termo/<termo>?page=<página>`. Preservados os produtos e o paginator da resposta pública.
- `royal-empty.json`: resposta real da busca pelo termo `gomosemresultados20260917`, página 1, com total 0.
- `nagumo-empty.json`: resposta real de `Search-UpdateGrid?q=gomosemresultados20260917&sz=50&start=0`, com count 0.

Não há cookies, tokens de clientes nem chaves de sessão nas fixtures. O servidor HTTP de testes simula a sessão anônima com valores explicitamente sanitizados. Os cenários negativos **alteram deliberadamente** as cópias dessas respostas: identidade/endereço, HTTP, JSON, atraso, ausência de preço/GTIN/estoque, repetição e quantidade mínima de promoção. Isso testa rejeição e condições de borda; não representa novas evidências comerciais.

`PublicCollectionIntegrationTests` executa ambos os adapters contra esse servidor local e persiste no PostgreSQL isolado, passando por Flyway, domínio, API, comparação e reprocessamento idempotente. Os testes unitários não acessam a internet. Os 20 vínculos comerciais revisados e suas URLs/imagens de evidência ficam em `src/main/resources/product-mappings/nagumo-royal.json`.
