# Manutenção e evolução

## Princípios para manter o padrão atual

1. Mantenha controllers focados em HTTP, validação de binding, `Model` e redirecionamento.
2. Coloque regras que combinam entidades ou múltiplas gravações em services transacionais.
3. Use repositories para persistência e consultas, com parâmetros nomeados em JPQL.
4. Use DTOs de formulário em vez de bind direto de entidade.
5. Carregue explicitamente associações necessárias à view; Open Session in View está desativado.
6. Preserve o fluxo Post/Redirect/Get para gravações bem-sucedidas.
7. Proteja toda nova rota no `SecurityConfig`, especialmente operações administrativas.
8. Evolua PostgreSQL por migration e mantenha o mapeamento compatível com SQLite.
9. Adicione testes proporcionais à regra alterada.
10. Documente o motivo de decisões não óbvias em JavaDoc ou comentário próximo ao código.

## Onde colocar cada tipo de código

| Necessidade | Local |
|---|---|
| endpoint/página | `controller/` |
| regra e transação | `service/` |
| entidade persistente | `entity/` |
| enum persistido | `enums/` |
| entrada/saída específica | `dto/` |
| consulta ao banco | `repository/` |
| autenticação/configuração | `config/` |
| tradução global de falhas | `exception/` |
| HTML | `resources/templates/<módulo>/` |
| estilos reutilizáveis | `resources/static/css/` |
| schema PostgreSQL | `resources/db/migration/` |
| seed SQLite | `resources/db/seed/` |

## Como adicionar uma funcionalidade

Antes de alterar:

- identifique regra, papel autorizado, entidade e telas afetadas;
- verifique se a mudança funciona nos dois profiles;
- defina o contrato HTTP e as mensagens de erro;
- avalie impacto em dashboard/relatórios e dados históricos;
- escreva teste que falhe com o comportamento anterior.

Sequência recomendada no desenho atual:

1. criar/alterar DTO e validações;
2. ajustar entidade e migration, quando houver persistência;
3. adicionar consulta no repository;
4. implementar regra no service e demarcar transação;
5. criar endpoint no controller;
6. proteger a URL no SecurityConfig;
7. criar/ajustar template e CSS/JavaScript;
8. testar service, MVC, segurança e integração;
9. atualizar documentação e changelog;
10. executar `mvn clean package` e, se aplicável, testes auxiliares.

## Como criar um Controller

Use `@Controller` para HTML/downloads ou `@RestController` para JSON. O projeto usa injeção por construtor com campos `final` e `@RequiredArgsConstructor`.

Responsabilidades esperadas:

- declarar rota e método HTTP;
- converter `@RequestParam`/`@PathVariable`;
- receber DTO com `@Valid` e `BindingResult`;
- preparar atributos de view;
- delegar a regra ao service;
- redirecionar após escrita e usar flash attributes.

Evite iniciar lógica transacional no controller. Existem acessos diretos a repositories em controllers atuais, mas novas operações de negócio com mais de uma responsabilidade devem ir para service.

Checklist de endpoint:

- método HTTP correto, sem escrita via GET;
- papel declarado no `SecurityConfig`;
- CSRF mantido para POST;
- validação e comportamento de erro documentados;
- IDs inexistentes convertidos em `EntityNotFoundException` quando necessário;
- resposta HTML, JSON ou binária coerente;
- JavaDoc com parâmetros, retorno e exceções relevantes;
- teste MockMvc para sucesso, validação, autorização e CSRF.

## Como criar um Service

Padrão atual:

- `@Service` e `@RequiredArgsConstructor`;
- `@Transactional(readOnly = true)` na classe quando a maioria é consulta;
- `@Transactional` nos métodos de escrita;
- exceptions de domínio atuais: `EntityNotFoundException` e `IllegalArgumentException`;
- retorno de entidades ou DTOs específicos, sem acesso lazy posterior não planejado.

Uma operação que grava entidade e histórico deve permanecer na mesma transação. Não capture exceções de integridade apenas para ocultá-las; o advice já traduz `DataIntegrityViolationException` para 409.

Para paginação/ordenação, limite tamanho e use allowlist de propriedades como `ChamadoService`.

## Como criar uma Entidade

1. Use `@Entity` e nome de tabela explícito.
2. Defina PK identity compatível com SQLite/PostgreSQL.
3. Expresse `nullable`, `length`, unicidade e nomes de coluna.
4. Prefira associações `LAZY` e carregamento explícito na consulta.
5. Avalie `equals/hashCode`; `Chamado` usa somente ID, mas outras entidades atualmente usam o comportamento do Lombok `@Data`.
6. Documente callbacks e campos derivados.
7. Adicione migration PostgreSQL versionada.
8. Verifique o comportamento do Hibernate SQLite.

Não dependa de `ddl-auto: update` em produção: no profile `docker`, o Hibernate apenas valida.

### Migrações

Para PostgreSQL, crie o próximo arquivo imutável:

```text
src/main/resources/db/migration/postgresql/V3__descricao_da_mudanca.sql
```

Use `common/` somente para SQL realmente compatível com a execução prevista em PostgreSQL; apesar do nome, essas migrations não rodam no SQLite atual. O SQLite recebe schema pelo Hibernate e dados por `db/seed/sqlite-data.sql`.

Nunca edite uma migration já aplicada em produção. Crie uma nova versão. Garanta que a migration inclui constraints e índices que as anotações não criam sob `ddl-auto: validate`.

## Como criar DTOs

- DTO de formulário: classe simples com Bean Validation e mensagens em português.
- Datas: use `@DateTimeFormat` compatível com o input HTML.
- Limites de texto: alinhe `@Size` com coluna, relatório e expectativa da UI.
- IDs relacionados: use `@NotNull` quando obrigatórios e resolva a entidade no service.
- DTO de saída: exponha somente campos consumidos pela view/API; converta dentro de transação ou após fetch explícito.

O Lombok reduz boilerplate, mas não substitui documentação do propósito e das regras do DTO.

## Como criar um Repository

- Estenda `JpaRepository<Entidade, Long>`.
- Use métodos derivados para filtros simples.
- Use JPQL com `@Query` e `@Param` para agregações/filtros complexos.
- Use `JOIN FETCH`/`@EntityGraph` quando a view acessará relações após a transação.
- Para intervalos de data, siga o padrão semiaberto (`>= início`, `< fimExclusive`).
- Para paginação com fetch joins, valide a consulta de contagem gerada e adicione `countQuery` explícita se a consulta crescer.
- Documente formato de `Object[]` em consultas agregadas ou prefira projection DTO ao introduzir consultas novas.

## Templates e frontend

Templates usam `layout/base`, atributos `pageTitle`/`activePage`, tokens CSS e componentes existentes.

Ao criar uma página:

- mantenha `lang="pt-BR"`, UTF-8 e layout comum;
- use `th:field` para formulários validados;
- não remova a integração que injeta CSRF;
- esconda ações administrativas com `sec:authorize`, além da regra server-side;
- preserve IDs únicos e atributos de acessibilidade em modais;
- trate falhas de `fetch` e não insira texto remoto com `innerHTML`;
- reutilize classes em `components.css` antes de criar estilos inline;
- considere que bibliotecas CDN podem estar indisponíveis.

O controlador global de modais usa `hidden`, `aria-hidden`, restauração de foco, Escape, clique no backdrop e trap de Tab. Novos modais de cadastro devem seguir o contrato `data-modal`, `data-modal-open` e `data-modal-close` coberto pelos testes.

## Segurança ao evoluir

Toda nova rota é autenticada pela regra final `anyRequest().authenticated()`, mas isso não a torna administrativa automaticamente.

- Adicione matcher de `ADMIN` antes da regra geral quando necessário.
- Mantenha regras mais específicas antes das abrangentes.
- Não desative CSRF para facilitar testes ou chamadas de formulário.
- Não coloque senhas em `application.yml`, `.env` versionado, testes reais ou logs.
- API externa futura exigirá decisão explícita sobre sessão/token, CORS, versionamento e contrato; `/api` atual é apenas auxiliar.
- Se adicionar upload/download, valide nome, tamanho, tipo e destino; use o cuidado de sanitização já aplicado aos downloads JCEF.

## Relatórios

Ao adicionar colunas:

- altere Excel e PDF de forma consistente;
- garanta fetch das associações usadas antes de sair do repository;
- ajuste larguras/percentuais e testes;
- mantenha nomes de planilha válidos e únicos;
- confira se o período exibido ao usuário corresponde ao limite exclusivo usado internamente.

## Testes existentes e onde ampliar

| Teste | Cobertura atual |
|---|---|
| `ChamadoServiceTest` | reabertura, remoção de equipamento, vínculo com prestador e cálculo de tempo |
| `DashboardServiceTest` | propagação do filtro por prestador |
| `RelatorioExcelServiceTest` | nomes de abas válidos/únicos |
| `ChamadoControllerTest` | conversão do período para limite exclusivo |
| `SecurityConfigTest` | login exigido, papel ADMIN, CSRF e senhas distintas |
| `ApplicationIntegrationTest` | SQLite, Thymeleaf, paginação, Problem Details, relatórios e associações inativas |
| `ModalContractTest` | contrato estático dos modais |
| `modal-browser-test.mjs` | comportamento em Edge headless, quando executado manualmente |
| `gerar-instalador.Tests.ps1` | encadeamento do JAR novo até o MSI |

Comando obrigatório de regressão Java:

```powershell
mvn clean test
```

Testes auxiliares:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\gerar-instalador.Tests.ps1
node .\src\test\js\modal-browser-test.mjs
```

O teste JavaScript requer Node.js e Microsoft Edge no caminho usado pelo script; ele não é executado pelo Maven.

Ao mudar banco PostgreSQL, adicione teste contra PostgreSQL real ou container no pipeline. A suíte atual de integração usa SQLite em memória e não valida as migrations em um servidor PostgreSQL.

## JavaDoc e comentários

- Documente classes públicas com papel arquitetural.
- Em métodos públicos, explique contrato, unidades, inclusão/exclusão de limites, efeitos transacionais e exceções.
- Use `@param`, `@return` e `@throws` quando agregarem informação que a assinatura não contém.
- Em repositories, descreva o formato e a ordem do retorno.
- Comentários internos devem explicar por que uma operação incomum existe; não narrar cada linha.
- Atualize JavaDoc quando uma regra mudar; documentação incorreta é pior que ausência.

## Versionamento e entrega

- A versão vem de `pom.xml` e é usada em JAR, MSI e interface.
- Após alterar código: testes, package e reinício/rebuild do modo usado.
- Para MSI distribuível, incremente a versão e gere novamente JAR/MSI/checksum.
- Para Docker, mantenha migrations aditivas e faça backup antes do deploy.
- Atualize `docs/CHANGELOG.md` com mudanças verificáveis; não derive histórico apenas de artefatos compilados.

## Checklist de revisão

- [ ] Regra está em service e transação adequada.
- [ ] DTO valida entrada e mensagens.
- [ ] Entidade, migration PostgreSQL e SQLite estão coerentes.
- [ ] Relações lazy necessárias são carregadas explicitamente.
- [ ] Endpoint e papel estão documentados/protegidos.
- [ ] CSRF permanece ativo.
- [ ] Templates não expõem ação indevida.
- [ ] Dashboard/relatórios foram avaliados.
- [ ] Testes de unidade, MVC, segurança e integração passam.
- [ ] JavaDoc e documentos foram atualizados.
- [ ] O pacote aplicável (JAR, Docker ou MSI) foi reconstruído.

