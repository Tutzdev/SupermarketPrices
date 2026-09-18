# Catálogo, listas e comparação — validação local

Validação em 17/09/2026, no banco persistente `gomo_acceptance`, mantendo as contas, assinaturas e listas existentes. O backup anterior à alteração está em `.local/backups/before-catalog-lists.dump` (fora do Git).

## Causas corrigidas

- O seletor da lista consultava apenas `products({ size: 100 })`: a primeira página ordenada por nome terminava na letra B. Agora há busca no servidor, debounce de 300 ms e páginas de 30 produtos, com navegação e contagem total. A comparação individual reutiliza o mesmo seletor.
- A interface condicionava todos os resultados à existência de `recommendation`. O backend devolvia `null` quando nenhuma loja cobria a lista inteira, junto de coberturas parciais que a tela ignorava. Agora todos os mercados da página aparecem, com cada produto, quantidade, preço, subtotal, data e motivo de ausência.
- A coleta estava restrita a marcas parceiras/termos de busca. Nagumo agora percorre 15 departamentos públicos além de MP-GERAL; Royal percorre a árvore de departamentos. Uma página curta da Nagumo não encerra a coleta se a origem ainda indica mais páginas.
- Alterações de itens invalidavam apenas a consulta da lista, mantendo comparações antigas em cache. Agora invalidam também comparações, recomendações e o resumo de listas.

## Comportamento

As listas permitem criar, renomear, adicionar/remover produtos, editar quantidades e excluir com confirmação. Operações persistem na conta. Falhas de operação são mostradas na interface, inclusive no diálogo de exclusão.

O endpoint `GET /stores/{id}/products` retorna somente produtos observados naquele mercado e sua última cotação avaliada pela política de validade/estoque. A página do mercado oferece busca e paginação.

Uma loja só recebe recomendação de compra completa quando cobre todos os itens. Sem isso, a interface mostra a maior cobertura parcial e seus faltantes. `combination` escolhe o menor preço de cada identidade de produto, multiplica quantidades, arredonda cada linha e agrupa por loja. A economia só é calculada contra uma alternativa completa comparável; frete e deslocamento não entram.

Identidades continuam conservadoras: GTIN válido ou vínculo explicitamente revisado. Nomes parecidos, embalagens e sabores não são mesclados automaticamente. A Nagumo não fornece GTIN no catálogo público; produtos sem vínculo confirmado podem permanecer separados entre fontes.

Registros inválidos não ganham preços inventados. O parser Royal rejeitou 1.065 registros, com motivos registrados na execução (incluindo unidade de venda que exige revisão), mantendo 8.506 observações válidas na coleta ampliada. Produtos pesáveis da Nagumo só entram como preço por kg quando a fórmula preço × peso médio reproduz o preço da porção informado pela própria fonte.

## Contagens após a coleta ampliada

Consultas SQL no banco persistente, às 23:18 de 17/09/2026:

| Indicador | Quantidade |
| --- | ---: |
| Produtos canônicos cadastrados | 16.122 |
| Mercados reais | 2 |
| Registros históricos de preços | 17.441 |
| Produtos observados na Nagumo | 7.636 |
| Produtos observados no Royal | 8.506 |
| Identidades compartilhadas verificadas | 20 |

O catálogo mantém também produtos indisponíveis e observações anteriores; não promete que todos estejam à venda agora. Na última observação de cada produto/loja, a Nagumo declarou 7.327 disponíveis e 309 indisponíveis; o Royal, 5.580 disponíveis e 2.926 indisponíveis. A mesma identidade compartilhada aparece no catálogo das duas lojas, por isso a soma por loja é maior que o total canônico.

A coleta ampliada Nagumo recebeu 7.883 entradas antes da deduplicação entre departamentos, criou 7.174 produtos e gravou 7.561 novas observações; oito registros foram rejeitados por preço/peso sem validação suficiente. O Royal recebeu 9.571 entradas, criou 8.281 produtos e gravou 8.506 observações. Ambas as execuções foram `PARTIAL`, com descartes explícitos, e preservaram todo o histórico anterior.

IDs das execuções: Nagumo `b0d4a395-14b8-4452-bf67-f7f05de98510`; Royal `f65e70f0-8091-44a2-afb2-e1c6ca2d7b42`. As coletas iniciais da ampliação que falharam por confiança TLS ou interpretação de paginação também permanecem no histórico administrativo, sem substituir preços válidos por dados vazios.

## Testes executados

- `mvnw.cmd verify`: 176 testes, zero falhas, erros ou testes ignorados. Inclui PostgreSQL real, Flyway, identidade, autenticação, propriedade das listas, histórico, promoções, paginação, estoque, busca sem acentos, catálogo por loja, compra dividida, arredondamento e falhas de coleta.
- Após o ajuste da configuração de catálogo completo, os testes de Royal, Nagumo e calculadora foram executados novamente com sucesso.
- `npm --prefix frontend run build` e `npm --prefix frontend run lint`: aprovados. O build informa apenas avisos de anotações de dependência do Zod removidas pelo bundler.
- Navegador real com a conta existente: criação de uma lista, inclusão de 20 produtos pela interface, quantidades 1–3, comparação, alteração de quantidade para 4,5 e restauração, remoção/reinclusão, renomeação e recarga. Uma lista temporária foi criada, sua exclusão cancelada e depois confirmada; as duas listas úteis permaneceram salvas.
- Paginação do seletor, busca por nome/código, catálogos das lojas e viewport de 390 px foram verificados. Não houve erro de console nem transbordamento horizontal da página; tabelas têm rolagem própria.
- Após as duas coletas, a busca retornou 16.122 produtos em 538 páginas de 30, incluindo a última página. Os totais da lista foram recalculados e novamente conferidos com `Decimal`, incluindo a soma dos mínimos por produto entre mercados. O backend foi reiniciado pelo script persistente, e a sessão existente continuou abrindo a lista e comparando sem novo login.
- `EXPLAIN (ANALYZE, BUFFERS)` da busca por “cafe”, com as mesmas colunas e ordenação da API, levou 3,583 ms de execução no banco local ampliado. O PostgreSQL escolheu o índice de ordenação para essa página de 30 resultados; o resultado é uma medição local, não uma promessa de latência em produção.

Lista deixada para conferência: **Compra variada - 20 produtos reais**, ID `d87b1f78-69f2-468f-af91-9bfb6e7ee791`. Contém arroz, biscoito, café, detergente, feijão, leite de coco, macarrão, óleo, papel alumínio, queijo, refrigerante, sabão, tomate, uva, vinho, iogurte, manteiga, farinha, sal e água de coco. A lista original **Compra da semana** foi preservada.

No primeiro cálculo verificado, Royal cobriu 20/20 itens por **R$ 618,93**. A combinação custou **R$ 617,31**, economia de **R$ 1,62**, com 19 itens no Royal e o refrigerante na Nagumo. Os totais foram conferidos independentemente com `Decimal` e arredondamento `HALF_UP`. Ao mudar o arroz de 1 para 4,5, o total completo passou a **R$ 718,65**; ao remover a água de coco de quantidade 2, passou a **R$ 594,95**. A restauração voltou ao total original.

## Arquivos principais

- Frontend: `product-picker.tsx`, `price-details.tsx`, `comparison-items.tsx`, `list-comparison.tsx`, páginas de listas/comparação/mercados, nova `store-catalog-page.tsx`, roteador, serviço da API, tipos e diálogo de confirmação.
- Backend: `ShoppingPriceCalculator`, `ShoppingComparisonService`, contratos de recomendação/combinação, `StoreCatalogService`, `StoreProductResponse`, `StoreController`, busca de produtos, clientes dos coletores e seleção administrativa de uma fonte.
- Persistência: migrations V13/V14 adicionam índices de ordenação e trigramas. Não removem nem substituem dados.
- Operação: propriedades, `.env.example`, Compose, `scripts/start-local.ps1`, README e documentação das fontes. O inicializador usa o mesmo PostgreSQL e os certificados confiáveis do Windows.
