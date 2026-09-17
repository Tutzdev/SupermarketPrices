# Fontes de dados e ingestão

O banco inicial contém somente o estado e as quatro cidades autorizadas pelo escopo. Não há fontes comerciais, lojas, produtos, estoques ou preços pré-cadastrados. Uma fonte, rede e loja são criadas somente depois de uma coleta real bem-sucedida. Os registros sintéticos usados nos testes estão exclusivamente em `src/test`.

As interfaces `ProductDataProvider`, `StoreDataProvider` e `PriceDataProvider` continuam definindo consultas paginadas. Os coletores automáticos implementam `SupermarketCollector` e entregam um catálogo normalizado ao mesmo domínio de ingestão. A ausência de uma integração não é convertida em dados inventados.

## Integrações investigadas

Situação verificada em 17/09/2026:

| Supermercado | Unidade solicitada | Situação | Fonte e limitação |
| --- | --- | --- | --- |
| Nagumo | 036-V.REDONDA, Ponte Alta | Ativa | E-commerce público `https://www.nagumo.com.br`, com seleção explícita da loja 36 e JSON paginado. A fonte estruturada estável encontrada cobre a categoria `MP-GERAL` ("Produtos Nagumo"), não o catálogo inteiro, e não fornece GTIN. |
| Supermarket | Aterrado | Não automatizada | O encarte oficial público encontrado declara que os preços não valem no Sul Fluminense. Portanto ele não representa a unidade Aterrado. |
| Royal Supermercados | Aterrado | Não automatizada | O e-commerce público possui produtos estruturados, mas sua lista oficial de retirada não oferece Aterrado; em Volta Redonda, a loja identificada é Retiro. Esses preços não são atribuídos a Aterrado. |
| Empório Royale | Volta Redonda | Não automatizada | O aplicativo público encontrado está marcado como desativado/em manutenção, aponta para uma operação inativa em Resende e não retorna lojas de retirada. |

O coletor Nagumo primeiro abre uma sessão pública, consulta as lojas, exige correspondência exata de ID, nome, cidade, bairro e endereço, seleciona a loja e somente depois consulta o catálogo. Uma mudança nessa identidade interrompe a coleta em vez de misturar filiais. Preços da bandeira `NGM_36_M` são armazenados com a condição "Meu Nagumo" e não substituem o preço comum na comparação.

Na validação de 17/09/2026, a fonte declarou 117 itens disponíveis e retornou 128 registros no total; os 11 adicionais estavam indisponíveis. O coletor conta os registros realmente recebidos e preserva essa disponibilidade, sem apresentar os itens indisponíveis como ofertas atuais.

## Execução automática e manual

A coleta roda diariamente às 05:30 em `America/Sao_Paulo`. O cron, fuso, intervalo entre coletores, timeouts, tentativas e intervalo entre requisições estão em `application.properties` e podem ser substituídos pelas variáveis documentadas em `.env.example`. Use `PRICE_COLLECTION_ENABLED=false` para desativar apenas o agendamento ou `NAGUMO_COLLECTION_ENABLED=false` para remover o coletor Nagumo.

Um administrador pode iniciar a mesma rotina sem reiniciar a aplicação:

```http
POST /api/v1/admin/collections
Authorization: Bearer <token-administrativo>
```

As execuções podem ser consultadas em `GET /api/v1/admin/collections` e `GET /api/v1/admin/collections/{id}`. Cada registro informa horários, status e contagens encontradas, criadas, atualizadas, ignoradas e com erro. Coletores rodam sequencialmente; uma falha é finalizada como `FAILED` e não impede o próximo coletor.

## Adicionar uma fonte real

1. Confirme a existência da fonte, a autorização de acesso e o que seus dados realmente representam. Registre a documentação e a URL da origem. Uma rede conhecida não comprova a existência de uma unidade em determinada cidade.
2. Para uma integração manual, cadastre a fonte por `POST /api/v1/admin/sources`. Um coletor automático pode registrá-la somente depois que a resposta real e a unidade forem validadas, como faz `CollectionCatalogService`.
3. Implemente `SupermarketCollector` em um pacote próprio da integração. Mantenha o protocolo externo, a seleção de filial e o parsing fora do domínio principal.
4. Configure credenciais externamente e defina timeouts de conexão e leitura. Valide a resposta da origem antes de transformá-la em observações. Falhas técnicas devem ser propagadas ou representadas explicitamente como indisponibilidade pelo contrato do provider, sem convertê-las em uma consulta vazia bem-sucedida.
5. Persista primeiro os registros necessários de catálogo, pelos serviços internos correspondentes, e somente depois envie observações de preços a `PriceService.appendObservation`. Não faça chamadas externas dentro da transação de persistência.
6. Execute testes de contrato com respostas controladas e verificadas da fonte, cobrindo falhas, paginação, identidade da loja, duplicação e dados inválidos. Adicione o coletor à rotina existente; não crie um scheduler específico para cada rede.

`ProviderResult` distingue `AVAILABLE` e `UNAVAILABLE`. Uma resposta disponível e vazia significa que a consulta foi concluída sem resultados. Uma resposta indisponível exige uma explicação e não pode conter itens ou cursor.

Não associe produtos automaticamente por semelhança de nome. Um GTIN válido e os identificadores já verificados da fonte permitem identificação conservadora; diferenças de marca, embalagem ou quantidade exigem conferência antes de vincular registros.

## Identidade e rastreabilidade do preço

Cada `PriceObservation` exige produto, loja, fonte, referência da observação, preço regular positivo, moeda `BRL`, data de coleta e disponibilidade declarada. Quando a fonte não informa estoque, use `UNKNOWN`.

`sourceReference` identifica **uma observação imutável dentro da fonte**. Não use apenas a URL de uma página que muda diariamente. Uma fonte que oferece um identificador de evento pode usá-lo diretamente. Para páginas mutáveis, o adapter precisa de uma referência estável da coleta, preservada em tentativas repetidas, vinculada à URL e ao instante realmente observado. Uma coleta nova deve ter uma referência nova. Não gere uma referência aleatória diferente a cada retry.

A combinação `(sourceId, sourceReference)` é única. Repetir o mesmo conteúdo retorna o registro original; repetir a referência com conteúdo diferente gera conflito. Correções devem ser novas observações com sua própria origem e referência, sem sobrescrever o histórico.

Os campos temporais têm significados distintos:

| Campo | Significado |
| --- | --- |
| `collectedAt` | Instante observado pela coleta; não pode estar no futuro. |
| `recordedAt` | Instante em que o backend registrou a observação. |
| `validUntil` | Validade explícita da observação de preço, quando fornecida. |
| `promotionValidUntil` | Fim conhecido da promoção, quando fornecido. |
| `promotionCondition` | Clube, aplicativo, CPF ou outra condição necessária para obter o preço. |

Os timestamps são persistidos em UTC com precisão de microssegundos, compatível com PostgreSQL. A normalização também participa da verificação de idempotência.

`regularPrice` e `promotionalPrice` usam `BigDecimal` com até duas casas decimais. Promoção, quando informada, deve ser positiva e menor que o preço regular. A ingestão não arredonda silenciosamente um valor com precisão inválida.

## Preços atuais e comparação

O endpoint de histórico retorna observações, inclusive preços antigos e promoções encerradas. Ele não afirma que esses valores continuam vigentes.

A comparação seleciona exatamente uma observação por produto e loja, ordenando por `collectedAt DESC`, `recordedAt DESC` e `id DESC`. Uma observação recebida mais tarde, mas coletada antes, não substitui a mais recente. Se a observação escolhida estiver expirada, não há recuo para um preço histórico mais conveniente.

O prazo configurado em `app.prices.max-age` limita a idade aceita do preço; o padrão é `P2D`. Quando `validUntil` informa um prazo menor, prevalece esse prazo. A expiração ocorre no próprio instante limite. O frescor é uma política operacional e não uma garantia de que o estabelecimento ainda praticará aquele preço.

Uma promoção só compõe o preço atual quando `promotionValidUntil` é conhecido, está no futuro e `promotionCondition` não está preenchido. Sem validade ou com uma condição especial, o valor promocional permanece visível no histórico e a comparação usa o valor regular. `promotionApplied` informa a decisão e `expiresAt` informa o limite do preço efetivamente utilizado.

O coletor gera uma referência determinística com data e conteúdo. Repetir a mesma coleta no mesmo dia não cria histórico duplicado. Uma observação diária nova é necessária mesmo sem alteração de valor porque comprova o instante de atualização e renova a validade diária declarada pela fonte. `collectedAt`, `status` e `expiresAt` permitem ao frontend diferenciar preço atual, expirado e ausente; `PRICE_MAX_AGE` controla o limite de stale quando a fonte não fornece prazo menor.

O contrato de `PriceQuote` diferencia:

| `status` | Efeito na comparação |
| --- | --- |
| `KNOWN` | Existe um preço aceito pela política temporal. Estoque ainda pode ser `UNKNOWN`. |
| `NO_OBSERVATION` | Não há observação para o produto/loja; preço e total da linha são `null`. |
| `EXPIRED` | A última observação perdeu validade; preço e total da linha são `null`. |
| `OUT_OF_STOCK` | A última observação válida informa indisponibilidade; a linha não compõe o subtotal. |

Ausência ou expiração de preço não afirmam falta de estoque. Nesses casos, a disponibilidade atual é `UNKNOWN`; a disponibilidade originalmente observada continua visível nos metadados históricos.

Nas listas, cada preço conhecido é multiplicado pela quantidade solicitada. O valor de cada linha é arredondado para centavos com `HALF_UP`, e o subtotal soma as linhas já arredondadas. `requestedItems`, `pricedItems` e `missingItems` contam linhas de produtos distintos, não a soma das quantidades.

`subtotalKnown` é `null` quando nenhuma linha tem preço. `completeShoppingList` indica que todas as linhas de uma lista não vazia possuem preços utilizáveis; não garante estoque quando a origem o declara desconhecido. Uma lista vazia tem subtotal `null` e nunca é marcada completa.

As lojas da comparação detalhada são paginadas por nome e ID, com até 100 lojas por requisição. Todos os preços da página são consultados em um único lote, limitado às lojas da página e aos produtos da lista.

A recomendação de compra usa um endpoint separado e consulta em lote todas as lojas elegíveis da cidade. Se a quantidade ultrapassar `RECOMMENDATION_MAX_STORES`, a API retorna 422 em vez de declarar uma vencedora parcial. Somente uma loja com preço utilizável para todos os itens pode ser recomendada; candidatos incompletos são apresentados separadamente como cobertura, nunca como vencedor.

## Contribuições de usuários

Uma contribuição aprovada também precisa de uma origem rastreável. Cadastre uma fonte administrativa que represente a própria plataforma, usando a URL real do ambiente, e configure seu UUID em `CONTRIBUTION_SOURCE_ID`. Não crie uma fonte fictícia em migration.

A aprovação publica a observação por `PriceService`, com `originType=USER_CONTRIBUTION`, uma referência determinística derivada do UUID da contribuição e vínculo único com o preço persistido. Repetir a aprovação não cria uma nova observação. Contribuições pendentes ou rejeitadas nunca chegam a `price_records` e, portanto, não participam das comparações ou alertas.
