# Segurança

## Modelo de segurança

A aplicação usa Spring Security com autenticação baseada em formulário, sessão HTTP e autorização por papéis. As contas são configuradas externamente e mantidas em memória; não existem entidades ou tabelas de usuário.

## Autenticação

`SecurityProperties` exige duas contas completas:

- `app.security.user`: recebe papel `ROLE_USER`;
- `app.security.admin`: recebe `ROLE_USER` e `ROLE_ADMIN`.

As propriedades vêm de variáveis de ambiente no perfil local ou de Docker secrets no Compose. A inicialização falha se:

- alguma conta, nome ou senha estiver ausente/em branco;
- uma senha tiver menos de 12 caracteres;
- os nomes forem iguais sem diferenciar maiúsculas/minúsculas;
- as senhas forem idênticas.

O `UserDetailsService` é um `InMemoryUserDetailsManager`. As senhas recebidas em texto são codificadas na inicialização com `DelegatingPasswordEncoder`; o encoder padrão criado por essa factory usa um identificador de algoritmo no hash. O texto original continua existindo na fonte externa que forneceu a propriedade, por isso arquivos e ambiente precisam ser protegidos.

O login usa `/login`, sucesso padrão `/dashboard` e falha `/login?error`. O logout invalida a sessão e remove `JSESSIONID`.

## Autorização

| Recurso | Anônimo | `USER` | `ADMIN` |
|---|---:|---:|---:|
| `/login`, `/access-denied`, `/error` | sim | sim | sim |
| `/css/**`, `/favicon.ico` | sim | sim | sim |
| `/actuator/health` | sim | sim | sim |
| dashboard, chamados e relatórios | não | sim | sim |
| `/api/**` | não | sim | sim |
| `/prestadores/**` | não | não | sim |
| `/equipamentos/**` | não | não | sim |
| `/configuracoes/**` | não | não | sim |
| `POST /chamados/*/excluir` | não | não | sim |

As regras são aplicadas por URL e método HTTP na `SecurityFilterChain`; não há `@PreAuthorize` ou method security. A interface também esconde ações administrativas com `sec:authorize`, mas isso é apenas uma conveniência visual: a barreira efetiva é o filtro de segurança.

## Comportamento para acesso não autenticado

- MVC: redirecionamento para `/login`.
- `/api/**`: `401 application/problem+json` com detalhe genérico.

Isso evita que uma chamada AJAX receba HTML de login como se fosse JSON.

## CSRF

CSRF não é desativado; permanece habilitado pela configuração padrão do Spring Security.

- Formulários POST processados pelo Thymeleaf/Spring recebem o token oculto automaticamente.
- Login, logout, criação, alteração e exclusão exigem token válido.
- Falta ou token inválido gera `403` antes do controller.
- Endpoints JSON atuais são apenas GET e não alteram estado.

O teste `SecurityConfigTest` verifica explicitamente que `POST /prestadores/salvar` sem CSRF é bloqueado e que a mesma chamada com token pode prosseguir.

## Sessão e cookie

Configuração encontrada em `application.yml`:

- timeout da sessão: 30 minutos;
- cookie `HttpOnly`: ativo;
- `SameSite=Lax`;
- `Secure`: controlado por `APP_COOKIE_SECURE`, padrão `false`;
- remoção de `JSESSIONID` no logout.

Em produção sob HTTPS, `APP_COOKIE_SECURE` deve ser `true`. O projeto não configura TLS no Tomcat nem inclui proxy reverso; essa camada precisa ser fornecida pela implantação.

## Validação de entrada

### Bean Validation

DTOs de formulário aplicam `@NotNull`, `@NotBlank` e `@Size` a IDs e textos. `@Valid` é usado nos POSTs de chamados, prestadores, equipamentos, setores, motivos e status.

### Validação de negócio

O service valida regras que não podem ser expressas apenas por anotações:

- referências precisam existir;
- equipamento precisa pertencer ao prestador;
- fechamento não pode anteceder abertura;
- edição de chamado concluído não pode mover abertura para depois do fechamento;
- novo status deve ser diferente do atual;
- paginação e campo de ordenação pertencem a limites/lista permitida;
- intervalo final não pode anteceder o inicial.

A allowlist de campos de ordenação impede que um parâmetro arbitrário seja repassado como propriedade JPA.

### Banco

O schema PostgreSQL aplica `NOT NULL`, unicidade e FKs. Conflitos são convertidos em HTTP 409 no MVC. JPQL usa parâmetros nomeados, reduzindo exposição a injeção SQL; não há montagem de SQL com entrada do usuário.

## Tratamento de erros e exposição de informação

- Erros MVC 500 mostram mensagem genérica; detalhes são gravados no log.
- Erros da API usam Problem Details genérico em falhas de parâmetro e de servidor.
- Entidade inexistente pode revelar apenas a identificação lógica presente na mensagem do service.
- Senhas não são registradas pelo código Java.
- O script PowerShell lê senhas como `SecureString`, limpa o buffer BSTR e zera variáveis locais ao final.

## Proteções de implantação

- O container da aplicação executa como usuário não-root UID 10001.
- A imagem final contém apenas JRE, não Maven/JDK de build.
- O PostgreSQL não publica porta no host.
- O papel do banco da aplicação é criado como `NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT`.
- Senhas Docker são montadas como secrets, não passadas diretamente como variáveis de ambiente.
- A senha do papel limitado pode ser rotacionada pelo serviço `db-credential-sync`.
- O MSI é acompanhado por SHA-256 e o instalador auxiliar valida o checksum antes de elevar privilégios.
- O download do WiX usado no build possui SHA-256 fixado no script.

## Boas práticas implementadas

- credenciais fora do código e sem defaults;
- contas administrativa e operacional separadas;
- senha mínima e proibição de credenciais iguais;
- codificação de senha antes de uso pelo Spring Security;
- autorização server-side por papel;
- CSRF ativo;
- cookie HTTP-only e SameSite;
- sessão invalidada no logout;
- API não autenticada responde JSON apropriado;
- DTOs com validação e allowlist de ordenação;
- mensagens de erro externas sem stack trace;
- banco Docker isolado e aplicação em container não-root;
- `.env`, `secrets/`, bancos e logs ignorados pelo Git.

## Limitações verificadas no código

Os itens abaixo não estão implementados e não devem ser presumidos:

- criação ou gestão dinâmica de usuários;
- persistência/auditoria de login, apesar do texto visual da tela afirmar que acessos são monitorados;
- MFA, recuperação de senha, expiração ou rotação automática de credenciais da aplicação;
- bloqueio por tentativas, rate limiting ou CAPTCHA;
- headers explícitos de Content Security Policy;
- Subresource Integrity nas dependências de CDN;
- TLS embutido;
- proteção criptográfica do SQLite em repouso;
- autorização por propriedade do registro;
- CORS configurado para consumo externo;
- logs de auditoria de edição/exclusão de cadastros e campos comuns do chamado.

As bibliotecas web são carregadas de Google Fonts, unpkg e jsDelivr. Sem CSP/SRI e sem cópia local, a disponibilidade e a cadeia de fornecimento desses recursos ficam fora do controle do artefato.

## Recomendações operacionais sem mudança de código

- mantenha `.env` e `secrets/` fora de versionamento e restrinja permissões de leitura;
- use valores aleatórios e exclusivos para as quatro senhas;
- publique a aplicação por proxy reverso HTTPS e defina `APP_COOKIE_SECURE=true`;
- mantenha `APP_BIND_ADDRESS=127.0.0.1` quando o proxy estiver no mesmo host;
- não publique a porta do PostgreSQL sem necessidade e controle de firewall;
- execute backup antes de recriar `pg_data` ou trocar estratégia de schema;
- revise logs, pois o nível da aplicação está configurado como `DEBUG` também no profile comum.

