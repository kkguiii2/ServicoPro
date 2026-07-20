# Endpoints HTTP

## Convenções

- Base URL padrão: `http://127.0.0.1:8080`.
- O context path é `/`.
- Rotas MVC retornam HTML ou redirecionamento; não constituem uma API REST de domínio.
- Formulários usam `application/x-www-form-urlencoded` e, em todos os POSTs, exigem o campo CSRF gerado pelo Spring Security.
- A API em `/api/**` retorna JSON e existe para apoiar a interface.
- Datas simples usam `yyyy-MM-dd`; data/hora de formulário usa `yyyy-MM-ddTHH:mm`.
- Intervalos de consulta são convertidos internamente para `[início 00:00, dia seguinte ao fim 00:00)`, tornando o dia final inclusivo.
- `USER` significa operador ou administrador; `ADMIN` significa somente administrador.

## Respostas de erro comuns

### MVC

| Situação | Resposta |
|---|---|
| usuário anônimo em rota protegida | `302` para `/login` |
| papel insuficiente ou CSRF inválido/ausente | `403` / página de acesso negado conforme processamento do Spring Security |
| entidade não encontrada | `404`, template `error/not-found` |
| argumento, período, paginação ou tipo inválido | `400`, template `error/general` |
| constraint única/FK violada | `409`, template `error/general` |
| exceção não tratada | `500`, template `error/general`, sem detalhes internos na página |

Erros de Bean Validation em formulários de criação/edição normalmente retornam `200` com o mesmo formulário e mensagens nos campos, em vez de uma página HTTP 400.

### API JSON

`/api/**` usa `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Parâmetros inválidos para a requisição."
}
```

| Situação | Status |
|---|---|
| não autenticado | `401` |
| sem papel permitido | `403` |
| parâmetro/tipo inválido | `400` |
| entidade não encontrada, caso uma operação chamada a produza | `404` |
| erro inesperado | `500` |

## Autenticação e sessão

### `GET /login`

- **Acesso:** público.
- **Parâmetros:** `error` e `logout` podem aparecer na query após falha/sucesso de autenticação.
- **Body:** nenhum.
- **Resposta:** `200`, template `auth/login`; se já autenticado, `302` para `/dashboard`.
- **Erros:** falha inesperada resulta no tratamento MVC padrão.

### `POST /login`

Endpoint fornecido pelo Spring Security.

- **Acesso:** público.
- **Body:** `username`, `password`, `_csrf`.
- **Resposta:** credenciais válidas geram sessão e `302` para a URL salva ou `/dashboard`; inválidas geram `302` para `/login?error`.
- **Erros:** CSRF ausente/inválido resulta em `403`.

### `POST /logout`

Endpoint fornecido pelo Spring Security.

- **Acesso:** usuário autenticado.
- **Body:** `_csrf`.
- **Resposta:** invalida a sessão, remove `JSESSIONID` e retorna `302` para `/login?logout`.
- **Erros:** CSRF ausente/inválido resulta em `403`.

### `GET /access-denied`

- **Acesso:** público.
- **Resposta:** `200` quando acessada diretamente, template `error/403`. Quando usada pelo fluxo de negação do Spring Security, a resposta original permanece uma negação de acesso.

## Dashboard

### `GET /` e `GET /dashboard`

- **Acesso:** `USER`.
- **Query:** `prestadorId` (`Long`, opcional).
- **Body:** nenhum.
- **Resposta:** `200`, template `dashboard/index`, com KPIs, quatro gráficos e os dez chamados mais recentes.
- **Erros:** `400` para tipo inválido; `404` quando o ID informado não existe.

## Chamados

### `GET /chamados`

- **Acesso:** `USER`.
- **Query:**

| Parâmetro | Tipo | Obrigatório | Padrão/regra |
|---|---|---|---|
| `prestadorId` | `Long` | não | igualdade |
| `status` | enum | não | `ABERTO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO` |
| `setorId` | `Long` | não | igualdade |
| `motivoId` | `Long` | não | igualdade |
| `equipamentoId` | `Long` | não | igualdade |
| `dataInicio` | data | não | início inclusivo |
| `dataFim` | data | não | dia final inclusivo |
| `conceito` | enum | não | valores de `ConceitoAvaliacao` |
| `numeroCh` | texto | não | contém, sem diferenciar maiúsculas/minúsculas |
| `page` | inteiro | não | `0`; não negativo |
| `size` | inteiro | não | `20`; entre 1 e 100 |
| `sortBy` | texto | não | `dataAbertura`; permitido: `id`, `numeroCh`, `dataAbertura`, `dataFechamento`, `status`, `createdAt` |
| `sortDir` | texto | não | `desc`; somente `asc` seleciona crescente, qualquer outro valor resulta em decrescente |

- **Resposta:** `200`, template `chamados/lista` com uma `Page<Chamado>`.
- **Erros:** `400` para enum/tipo, período, paginação ou campo de ordenação inválido.

### `GET /chamados/novo`

- **Acesso:** `USER`.
- **Resposta:** `200`, formulário vazio `chamados/form` e listas ativas de apoio.

### `POST /chamados/novo`

- **Acesso:** `USER`.
- **Body:**

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `numeroCh` | texto | não | máximo 50 |
| `prestadorId` | `Long` | sim | deve existir |
| `equipamentoId` | `Long` | não | deve existir e pertencer ao prestador |
| `setorId` | `Long` | sim | deve existir |
| `motivoId` | `Long` | sim | deve existir |
| `descricaoAtendimento` | texto | sim | não branco; máximo 10.000 |
| `dataAbertura` | data/hora | sim | ISO local |
| `conceito` | enum | não | conceito válido |
| `observacao` | texto | não | máximo 5.000 |

- **Resposta:** sucesso: `302` para `/chamados/{id}` com flash message; validação: `200` com o formulário.
- **Erros:** `400` para equipamento de outro prestador; `404` para IDs inexistentes; `409` para violação persistente.

### `GET /chamados/{id}`

- **Acesso:** `USER`.
- **Path:** `id` (`Long`).
- **Resposta:** `200`, template `chamados/detalhe` com entidade detalhada, histórico descendente e formulário de status.
- **Erros:** `400` para ID incompatível; `404` se não existir.

### `GET /chamados/{id}/editar`

- **Acesso:** `USER`.
- **Path:** `id` (`Long`).
- **Resposta:** `200`, formulário preenchido. O prestador/equipamento atual é incluído mesmo se estiver inativo.
- **Erros:** `400` para tipo inválido; `404` se não existir.

### `POST /chamados/{id}/editar`

- **Acesso:** `USER`.
- **Path:** `id` (`Long`).
- **Body:** mesmos campos e validações de `POST /chamados/novo`.
- **Resposta:** sucesso: `302` para o detalhe; validação: `200` com o formulário.
- **Erros:** `400` se o equipamento não pertencer ao prestador ou se, em chamado concluído, a nova abertura ficar após o fechamento; `404` para chamado/associação inexistente; `409` para conflito persistente.

### `POST /chamados/{id}/status`

- **Acesso:** `USER`.
- **Path:** `id` (`Long`).
- **Body:**

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `novoStatus` | enum | sim | deve ser diferente do atual |
| `observacao` | texto | sim | não pode ser branca |
| `dataFechamento` | data/hora | somente funcionalmente para `CONCLUIDO` | quando ausente na conclusão, usa o instante atual |

- **Resposta:** `302` para o detalhe. Erro de Bean Validation também redireciona, com flash message genérica.
- **Erros:** `400` para estado igual ao atual ou fechamento anterior à abertura; `404` se o chamado não existir.

### `POST /chamados/{id}/excluir`

- **Acesso:** `ADMIN`.
- **Path:** `id` (`Long`).
- **Body:** `_csrf`.
- **Resposta:** exclui histórico e chamado na mesma transação; `302` para `/chamados`.
- **Erros:** `404` se o chamado não existir; `403` sem papel/CSRF.

## Prestadores

Todas as rotas abaixo exigem `ADMIN`.

### `GET /prestadores`

- **Resposta:** `200`, template `prestadores/lista`, incluindo ativos e inativos.

### `POST /prestadores/salvar`

- **Body:** `nome` obrigatório/não branco/máximo 255; `descricao` opcional/máximo 2.000; `_csrf`.
- **Resposta:** sucesso `302` para `/prestadores`; validação `200` com a listagem.
- **Erros:** `409` para nome duplicado.

### `POST /prestadores/{id}/desativar`

- **Path:** `id` (`Long`).
- **Body:** `_csrf`.
- **Resposta:** define `ativo=false`; `302` para `/prestadores`.
- **Erros:** `404` se não existir.

### `POST /prestadores/{id}/ativar`

- **Path:** `id` (`Long`).
- **Body:** `_csrf`.
- **Resposta:** define `ativo=true`; `302` para `/prestadores`.
- **Erros:** `404` se não existir.

Não há endpoint de exclusão de prestador, apesar de existir um método de service não exposto.

## Equipamentos

Todas as rotas abaixo exigem `ADMIN`.

### `GET /equipamentos`

- **Query:** `prestadorId` (`Long`, opcional).
- **Resposta:** `200`, template `equipamentos/lista`. Com filtro, mostra equipamentos ativos do prestador; sem filtro, mostra todos.
- **Erros:** `400` para tipo inválido.

### `POST /equipamentos/salvar`

- **Body:** `prestadorId` obrigatório; `nome` obrigatório/máximo 255; `modelo` e `numeroSerie` opcionais/máximo 255; `_csrf`.
- **Resposta:** sucesso `302` para `/equipamentos`; validação `200` com a listagem.
- **Erros:** `404` se o prestador não existir; `409` para nome repetido no mesmo prestador.

### `POST /equipamentos/{id}/excluir`

- **Path:** `id` (`Long`).
- **Body:** `_csrf`.
- **Resposta:** exclusão física; `302` para `/equipamentos`.
- **Erros:** `409` se houver chamados referenciando o equipamento; `403` sem papel/CSRF.

## Configurações: setores e motivos

Todas as rotas abaixo exigem `ADMIN`.

### `GET /configuracoes`

- **Resposta:** `200`, template `configuracoes/index` com setores e motivos ordenados.

### `POST /configuracoes/setores/salvar`

- **Body:** `nome` obrigatório/não branco/máximo 255; `_csrf`.
- **Resposta:** sucesso `302`; validação `200` com a página.
- **Erros:** `409` para nome duplicado.

### `POST /configuracoes/setores/{id}/excluir`

- **Path:** `id` (`Long`).
- **Body:** `_csrf`.
- **Resposta:** exclusão física e `302`.
- **Erros:** `409` se o setor estiver em uso.

### `POST /configuracoes/motivos/salvar`

- **Body:** `descricao` obrigatória/não branca/máximo 255; `_csrf`.
- **Resposta:** sucesso `302`; validação `200` com a página.
- **Erros:** `409` para descrição duplicada.

### `POST /configuracoes/motivos/{id}/excluir`

- **Path:** `id` (`Long`).
- **Body:** `_csrf`.
- **Resposta:** exclusão física e `302`.
- **Erros:** `409` se o motivo estiver em uso.

## Relatórios

### `GET /relatorios`

- **Acesso:** `USER`.
- **Resposta:** `200`, template `relatorios/index` com prestadores ativos e formulários de período.

### `GET /relatorios/excel`

- **Acesso:** `USER`.
- **Query:** `prestadorIds` (`Long`, repetível/opcional), `dataInicio` (obrigatória), `dataFim` (obrigatória).
- **Resposta:** `200`, MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, anexo `Controle_Servico_yyyyMMdd.xlsx`.
- **Comportamento:** lista ausente/vazia seleciona todos os prestadores ativos; IDs repetidos são deduplicados pelo service XLSX.
- **Erros:** `400` para datas ausentes/inválidas ou fim anterior ao início; `404` para prestador inexistente; `500` se a escrita do workbook falhar.

### `GET /relatorios/pdf`

- **Acesso:** `USER`.
- **Query:** igual ao endpoint Excel.
- **Resposta:** `200`, MIME `application/pdf`, anexo `Relatorio_Servico_yyyy-MM-dd.pdf`.
- **Comportamento:** lista ausente/vazia seleciona todos os prestadores ativos. Diferente do XLSX, o service PDF percorre a lista recebida sem deduplicá-la.
- **Erros:** `400` para período inválido; `404` para prestador inexistente; `500` para falha de geração.

## API JSON auxiliar

Todas as rotas exigem `USER` e aceitam apenas GET.

### `GET /api/equipamentos`

- **Query:** `prestadorId` (`Long`, obrigatório), `incluirId` (`Long`, opcional).
- **Resposta:** `200`:

```json
[
  { "id": 1, "nome": "Impressora HP — HP LaserJet Pro" }
]
```

- **Comportamento:** retorna equipamentos ativos do prestador, ordenados por nome; `incluirId` adiciona o equipamento atual caso pertença ao prestador e esteja fora da lista ativa. Prestador inexistente retorna lista vazia.
- **Erros:** `400` para parâmetro ausente ou incompatível.

### `GET /api/prestadores`

- **Parâmetros/body:** nenhum.
- **Resposta:** `200`, lista de ativos:

```json
[{ "id": 1, "nome": "BRASILINE" }]
```

### `GET /api/setores`

- **Parâmetros/body:** nenhum.
- **Resposta:** `200`, lista de ativos:

```json
[{ "id": 1, "nome": "TI" }]
```

### `GET /api/motivos`

- **Parâmetros/body:** nenhum.
- **Resposta:** `200`, todos os motivos ordenados:

```json
[{ "id": 1, "descricao": "Falha de rede" }]
```

## Observabilidade

### `GET /actuator/health`

- **Acesso:** público.
- **Resposta:** `200` com JSON de saúde quando a aplicação está disponível; pode retornar status não saudável conforme o Actuator.
- **Uso:** healthcheck da imagem Docker consulta `http://127.0.0.1:8080/actuator/health` dentro do container.

Somente o endpoint `health` do Actuator está exposto. `/error` é o endpoint interno de erro do Spring Boot e não é um contrato funcional da aplicação.

