# ServiçoPro — Controle de Serviço

## Visão geral

O ServiçoPro é uma aplicação interna para registrar, acompanhar e auditar chamados atendidos por prestadores de serviço. O sistema mantém cadastros de prestadores, equipamentos, setores e motivos; controla o ciclo de vida do chamado; calcula reincidência e tempo de atendimento; apresenta indicadores; e exporta relatórios em XLSX e PDF.

O mesmo código possui dois modos de execução:

- **local/desktop**: Spring Boot, SQLite e uma janela Swing com navegador JCEF;
- **servidor**: aplicação web e PostgreSQL executados por Docker Compose.

Não existe um frontend separado. As páginas são renderizadas no servidor com Thymeleaf e usam JavaScript no navegador apenas para gráficos, seletores dinâmicos, datas e comportamento visual.

## Objetivo

Centralizar o acompanhamento de atendimentos de TI prestados por empresas terceiras, preservando o histórico de mudanças de status e permitindo análise por prestador, equipamento, setor, motivo, período, conceito e reincidência.

## Documentação

- [Arquitetura](architecture.md)
- [Banco de dados](database.md)
- [Endpoints](api.md)
- [Regras de negócio](business-rules.md)
- [Segurança](security.md)
- [Implantação](deployment.md)
- [Manutenção e evolução](maintenance.md)
- [Histórico de alterações](CHANGELOG.md)

## Tecnologias utilizadas

| Área | Tecnologia encontrada no projeto |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Web | Spring MVC e Tomcat embarcado |
| Views | Thymeleaf, Layout Dialect e extras do Spring Security |
| Persistência | Spring Data JPA e Hibernate |
| Banco local | SQLite JDBC 3.45.3.0 |
| Banco em Docker | PostgreSQL 16 Alpine |
| Migrações | Flyway, somente no perfil `docker` |
| Segurança | Spring Security 6, sessão e formulário de login |
| Validação | Jakarta Bean Validation |
| Relatórios | Apache POI 5.2.5 e iText 7.2.6 |
| Desktop | Swing e JCEF 135.0.20 |
| Build | Maven e Spring Boot Maven Plugin |
| Empacotamento Windows | `jpackage`, WiX 3.14.1 e PowerShell |
| Containers | Docker multi-stage e Docker Compose |
| Testes | JUnit 5, Mockito, MockMvc, AssertJ, PowerShell e teste opcional com Edge headless |
| UI no navegador | Chart.js, Alpine.js, Flatpickr e Phosphor Icons via CDN |

## Estrutura do projeto

```text
controle-servico/
├── docker/postgres/                 # inicialização, healthcheck e rotação da senha do PostgreSQL
├── docs/                            # documentação técnica
├── src/main/java/com/empresa/controleservico/
│   ├── config/                      # autenticação, autorização e propriedades de credenciais
│   ├── controller/                  # endpoints MVC e API JSON auxiliar
│   ├── dto/                         # modelos de formulário e projeções de leitura
│   ├── entity/                      # entidades JPA
│   ├── enums/                       # estados e conceitos persistidos
│   ├── exception/                   # tradução de exceções MVC e REST
│   ├── repository/                  # acesso a dados e consultas analíticas
│   └── service/                     # transações e regras de negócio
├── src/main/resources/
│   ├── db/migration/                # schema PostgreSQL e carga inicial comum
│   ├── db/seed/                     # carga inicial do SQLite
│   ├── static/                      # favicon e CSS
│   ├── templates/                   # páginas Thymeleaf
│   └── application*.yml             # configuração comum, local e Docker
├── src/test/                        # testes Java e JavaScript
├── tests/                           # teste estrutural do gerador MSI
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── configurar-credenciais.ps1       # grava credenciais no ambiente do usuário Windows
├── gerar-instalador.ps1             # build, JCEF offline, JAR, MSI e checksum
└── instalar-como-administrador.bat  # valida e instala o MSI
```

`target/`, `dist/`, `.installer/`, `.tools/`, JARs, MSIs e o banco em `data/` são artefatos gerados ou dados de execução, não código-fonte. A pasta `backend/` existente no workspace contém apenas artefatos de build e não compõe a aplicação-fonte.

## Pré-requisitos

### Execução local pelo Maven

- JDK 17 ou superior;
- Maven 3.9 ou superior;
- Windows com ambiente gráfico para a janela desktop; em ambiente headless, apenas o servidor HTTP é iniciado;
- acesso à internet na primeira inicialização do JCEF quando o runtime não estiver empacotado.

### Execução por Docker

- Docker Engine com o plugin `docker compose`;
- quatro arquivos locais de segredo, legíveis pelo Docker;
- porta TCP do host escolhida para publicação da aplicação.

## Configuração das credenciais

Há duas contas obrigatórias e distintas:

| Conta | Papel | Acesso |
|---|---|---|
| Operador | `USER` | dashboard, chamados, relatórios e API auxiliar |
| Administrador | `USER`, `ADMIN` | tudo que o operador acessa, cadastros mestres e exclusão de chamados |

Os nomes não podem ser iguais, as senhas não podem ser iguais e cada senha deve ter pelo menos 12 caracteres. Não há valor padrão no código.

No Windows, a forma guiada é:

```powershell
.\configurar-credenciais.ps1
```

O script grava `APP_USER_USERNAME`, `APP_USER_PASSWORD`, `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` nas variáveis de ambiente do usuário atual. Feche e reabra o terminal antes de iniciar a aplicação por outro processo.

Para uma sessão temporária:

```powershell
$env:APP_USER_USERNAME = "operador"
$env:APP_USER_PASSWORD = "substitua-por-senha-forte"
$env:APP_ADMIN_USERNAME = "administrador"
$env:APP_ADMIN_PASSWORD = "substitua-por-outra-senha"
```

No Docker, somente os nomes são variáveis comuns. As senhas são montadas como secrets a partir dos arquivos indicados no `.env`; consulte [Implantação](deployment.md#credenciais-e-secrets).

## Como executar localmente

Na raiz `controle-servico/`, configure as quatro credenciais e execute:

```powershell
mvn spring-boot:run
```

O perfil padrão é `local`. A aplicação escuta em `127.0.0.1:8080` e abre a interface JCEF quando há ambiente gráfico. A interface web também pode ser acessada em:

```text
http://127.0.0.1:8080
```

O bootstrap local usa, nesta ordem:

1. `APP_DB_PATH`, se definido como propriedade de sistema ou variável de ambiente;
2. `%ProgramData%\ControleServico\data\banco.db`, quando `ProgramData` existe;
3. `./data/banco.db`, como fallback.

Ele cria as pastas `data`, `backup`, `logs` e `config` no diretório-base e habilita o modo WAL do SQLite. `APP_DB_URL` pode substituir integralmente a URL JDBC, principalmente em testes.

## Como compilar

```powershell
mvn clean package
```

O comando executa os testes Maven e produz:

```text
target/controle-servico-1.0.0.jar
```

Para executar o JAR compilado no perfil local:

```powershell
java -Dspring.profiles.active=local -jar target/controle-servico-1.0.0.jar
```

O projeto também possui `gerar-instalador.bat`, que valida JDK/Maven/ícones, executa `mvn clean package`, baixa e verifica o WiX, prepara o JCEF offline, publica o JAR, gera o MSI e cria seu checksum SHA-256. Esse fluxo é exclusivo do Windows.

## Como rodar com Docker

O repositório contém um `.env` local ignorado pelo Git, mas não contém um `.env.example`. Crie ou revise um `.env` sem versionar segredos:

```dotenv
DB_NAME=controle_servico
DB_USER=controle_app
APP_PORT=8080
APP_BIND_ADDRESS=127.0.0.1
APP_USER_USERNAME=operador
APP_ADMIN_USERNAME=administrador
APP_COOKIE_SECURE=false
POSTGRES_ADMIN_PASSWORD_FILE=./secrets/postgres-admin.txt
APP_DB_PASSWORD_FILE=./secrets/app-db.txt
APP_USER_PASSWORD_FILE=./secrets/app-user.txt
APP_ADMIN_PASSWORD_FILE=./secrets/app-admin.txt
```

Crie os quatro arquivos apontados, cada um contendo somente a respectiva senha, e então execute:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

Com os valores acima, acesse `http://127.0.0.1:8080`. O primeiro build executa o build Maven dentro da imagem.

## Como o banco é configurado

### Local

O perfil `local` usa SQLite, `ddl-auto: update`, inicialização SQL em todas as partidas e a carga `db/seed/sqlite-data.sql`, escrita com `INSERT OR IGNORE`. Flyway fica desativado.

### Docker/servidor

O Compose cria três serviços:

- `postgres`: PostgreSQL e volume persistente `pg_data`;
- `db-credential-sync`: sincroniza a senha do papel limitado antes da aplicação;
- `app`: Spring Boot no perfil `docker`.

Na primeira criação do volume, o script `01-create-app-role.sh` cria o usuário limitado e o banco. A aplicação valida o mapeamento JPA com `ddl-auto: validate`; o Flyway cria o schema e carrega os dados iniciais.

## Variáveis de ambiente

| Variável | Uso | Padrão no código/Compose |
|---|---|---|
| `APP_USER_USERNAME` | login do operador | obrigatório |
| `APP_USER_PASSWORD` | senha do operador no perfil local | obrigatório |
| `APP_ADMIN_USERNAME` | login do administrador | obrigatório |
| `APP_ADMIN_PASSWORD` | senha do administrador no perfil local | obrigatório |
| `APP_COOKIE_SECURE` | marca `Secure` no cookie de sessão | `false` |
| `SERVER_ADDRESS` | endereço interno de escuta do Spring | `127.0.0.1`; Compose define `0.0.0.0` |
| `SPRING_PROFILES_ACTIVE` | perfil ativo | `local`; Compose define `docker` |
| `APP_DB_PATH` | caminho do SQLite | definido pelo bootstrap local |
| `APP_DB_URL` | URL JDBC local completa | `jdbc:sqlite:${APP_DB_PATH}?foreign_keys=on` |
| `DB_HOST` | host PostgreSQL visto pela aplicação | `postgres` |
| `DB_PORT` | porta PostgreSQL na rede Docker | `5432` |
| `DB_NAME` | banco PostgreSQL | `controle_servico` |
| `DB_USER` | papel limitado da aplicação | `controle_app` |
| `APP_PORT` | porta publicada no host pelo Compose | `8080` |
| `APP_BIND_ADDRESS` | endereço do host usado na publicação | `127.0.0.1` |
| `POSTGRES_ADMIN_PASSWORD_FILE` | arquivo da senha administrativa do PostgreSQL | obrigatório no Compose |
| `APP_DB_PASSWORD_FILE` | arquivo da senha do papel da aplicação | obrigatório no Compose |
| `APP_USER_PASSWORD_FILE` | arquivo da senha do operador | obrigatório no Compose |
| `APP_ADMIN_PASSWORD_FILE` | arquivo da senha do administrador | obrigatório no Compose |

`SPRING_CONFIG_IMPORT=configtree:/run/secrets/` é definido pelo Compose. Os secrets são montados com nomes compatíveis com propriedades Spring, como `spring.datasource.password` e `app.security.user.password`.

## Preciso reinstalar após alterar o código?

Depende da forma de execução:

- **Maven/desenvolvimento:** não reinstale. Recompile ou reinicie `mvn spring-boot:run`; o DevTools ajuda no ciclo de desenvolvimento, mas mudanças estruturais ainda podem exigir reinício.
- **JAR:** execute novamente `mvn clean package` e substitua/reinicie o processo que usa o JAR.
- **Docker:** reconstrua e recrie a aplicação com `docker compose up --build -d app`. O volume `pg_data` permanece; migrations novas são aplicadas pelo Flyway.
- **MSI:** gere um novo instalador e faça a atualização da instalação. Para distribuição controlada, incremente a versão em `pom.xml`; o script usa essa versão no nome e no pacote.

Alterar somente os dados no banco não exige recompilar ou reinstalar. Alterar credenciais exige reiniciar a aplicação para recriar os usuários em memória.

## Posso usar localmente e em servidor?

Sim. O perfil `local` foi implementado para uso desktop/SQLite. O perfil `docker` foi implementado para servidor web/PostgreSQL. Eles não compartilham automaticamente os mesmos dados: cada perfil usa seu próprio banco.

## Em servidor, preciso baixar o banco ou acessar `127.0.0.1`?

Não é necessário instalar PostgreSQL manualmente quando se usa o Compose: a imagem `postgres:16-alpine` é baixada pelo Docker e o banco roda no container `postgres`.

Os endereços têm funções diferentes:

- **aplicação → banco:** usa `postgres:5432`, nome DNS interno do Compose;
- **navegador no próprio servidor → aplicação:** usa `http://127.0.0.1:8080` com a configuração padrão;
- **navegador em outro computador → servidor:** `127.0.0.1` apontaria para o próprio computador do usuário, não para o servidor. Use o domínio/IP do servidor por meio de um proxy reverso HTTPS ou um túnel seguro.

O PostgreSQL não publica porta no host; portanto, `127.0.0.1:5432` não funciona com este Compose e não deve ser necessário para a aplicação. O navegador nunca acessa o banco diretamente.

