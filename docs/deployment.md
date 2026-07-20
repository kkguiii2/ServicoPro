# Implantação

## Modos suportados pelo código

| Modo | Perfil | Banco | Interface | Uso correspondente |
|---|---|---|---|---|
| desenvolvimento/local | `local` | SQLite | web + janela JCEF se houver tela | estação Windows ou execução local |
| JAR local | `local` | SQLite | web + JCEF | execução manual do artefato |
| MSI | `local` forçado pelo pacote | SQLite | JCEF | instalação Windows |
| Docker Compose | `docker` | PostgreSQL | web | servidor |

O perfil padrão é `local`. O Compose define explicitamente `SPRING_PROFILES_ACTIVE=docker`.

## Perfis Spring

### Configuração comum: `application.yml`

- Thymeleaf sem cache por padrão;
- datas MVC em padrão brasileiro;
- Jackson sem timestamps e tolerante a propriedades desconhecidas;
- servidor em `127.0.0.1:8080` por padrão;
- sessão de 30 minutos e cookie HTTP-only/SameSite Lax;
- duas contas externas obrigatórias;
- apenas o health endpoint do Actuator exposto;
- log da aplicação em `DEBUG`.

### Perfil `local`

- driver SQLite;
- URL `APP_DB_URL` ou `jdbc:sqlite:${APP_DB_PATH}?foreign_keys=on`;
- `PRAGMA foreign_keys=ON` ao abrir conexões do pool;
- Hibernate `ddl-auto: update`;
- Flyway desativado;
- seed SQLite executado sempre com instruções idempotentes;
- Open Session in View desativado;
- `DesktopLauncher` habilitado.

### Perfil `docker`

- driver PostgreSQL;
- URL formada por `DB_HOST`, `DB_PORT` e `DB_NAME`;
- usuário limitado em `DB_USER` e senha vinda de config tree;
- Hibernate `ddl-auto: validate`;
- Flyway habilitado nas pastas `postgresql` e `common`;
- inicialização SQL genérica desativada;
- cache Thymeleaf habilitado;
- `DesktopLauncher` não é criado.

## Deploy local pelo Maven

### 1. Validar ferramentas

```powershell
java -version
mvn -version
```

O compilador do projeto usa `release 17`.

### 2. Configurar contas

```powershell
.\configurar-credenciais.ps1
```

Ou configure as quatro variáveis da sessão:

```powershell
$env:APP_USER_USERNAME = "operador"
$env:APP_USER_PASSWORD = "senha-forte-com-12-ou-mais"
$env:APP_ADMIN_USERNAME = "administrador"
$env:APP_ADMIN_PASSWORD = "outra-senha-forte-e-distinta"
```

### 3. Escolher o banco local, se necessário

Sem configuração, o bootstrap usa `%ProgramData%\ControleServico\data\banco.db` no Windows ou `./data/banco.db` como fallback.

Para escolher explicitamente:

```powershell
$env:APP_DB_PATH = "C:\dados\ControleServico\banco.db"
```

`APP_DB_URL` substitui a URL JDBC completa e tem precedência na configuração do datasource.

### 4. Iniciar

```powershell
mvn spring-boot:run
```

Valide `http://127.0.0.1:8080/actuator/health` e acesse `http://127.0.0.1:8080`.

## Build e execução do JAR

```powershell
mvn clean package
java -Dspring.profiles.active=local -jar .\target\controle-servico-1.0.0.jar
```

`mvn clean package` executa a suíte Maven. O JAR Spring Boot inclui as dependências Java, mas o perfil local pode baixar o runtime JCEF se ele ainda não estiver em `%USERPROFILE%\.jcef_controle_servico` e não tiver sido empacotado pelo MSI.

## Geração e instalação do MSI

Execute no Windows:

```powershell
.\gerar-instalador.bat
```

Fluxo implementado pelo script:

1. valida Windows, `pom.xml`, `setting.ico`, `setting.png`, Maven e JDK/jpackage 17+;
2. lê artifact ID, versão e nome efetivo do JAR no Maven;
3. remove artefatos publicados da mesma versão e diretórios temporários;
4. executa `mvn clean package`;
5. valida entradas e manifesto do JAR executável;
6. localiza ou baixa WiX 3.14.1 e verifica o SHA-256 fixado;
7. monta o classpath e prepara/cacheia o runtime JCEF offline;
8. executa `jpackage --type msi` com perfil `local` e UUID de upgrade fixo;
9. publica JAR, MSI e checksum SHA-256 na raiz.

Para instalar, `instalar-como-administrador.bat` seleciona o checksum mais recente, valida o MSI correspondente, eleva apenas o `msiexec` e depois executa a configuração de credenciais como usuário atual.

## Dockerfile

O build possui duas etapas:

1. `maven:3.9-eclipse-temurin-17-alpine`: baixa dependências e executa `mvn -B clean package`;
2. `eclipse-temurin:17-jre-alpine`: copia o JAR como `/app/app.jar` e executa com UID 10001.

A imagem expõe 8080 e possui healthcheck HTTP no Actuator. Como o comando Maven não usa `-DskipTests`, testes fazem parte do build da imagem.

## Docker Compose

### Serviços

#### `postgres`

- imagem `postgres:16-alpine`;
- reinício `unless-stopped`;
- senha administrativa por secret;
- volume persistente `pg_data`;
- scripts de inicialização montados somente para leitura;
- sem porta publicada;
- healthcheck confirma a existência do papel e do banco da aplicação.

#### `db-credential-sync`

- aguarda o PostgreSQL saudável;
- lê as senhas administrativa e da aplicação;
- executa `ALTER ROLE` no papel limitado;
- termina com sucesso antes de liberar `app`;
- permite rotacionar a senha sem recriar o volume.

#### `app`

- construída pelo Dockerfile local;
- aguarda banco saudável e sincronização concluída;
- usa perfil `docker` e config tree;
- conecta a `postgres:5432` pela rede interna;
- publica 8080 no endereço/porta configurados do host;
- participa de `frontend` e `backend`.

### Redes

- `backend` é `internal: true` e reúne PostgreSQL, sincronizador e aplicação.
- `frontend` contém a aplicação e permite a frente publicada.
- O PostgreSQL não tem mapeamento `ports`, portanto só é resolvido como `postgres` entre containers ligados a `backend`.

## Credenciais e secrets

O `.dockerignore` exclui `.env`, `.env.*` (exceto um possível `.env.example`), `secrets`, bancos, logs e artefatos. O `.gitignore` também exclui `.env` e `secrets/`. No estado inspecionado não existe `.env.example`; use o modelo seguro em [README](README.md#como-rodar-com-docker).

Cada arquivo deve conter somente um valor:

```text
secrets/
├── postgres-admin.txt
├── app-db.txt
├── app-user.txt
└── app-admin.txt
```

Mapeamento:

| Arquivo apontado por | Destino/consumidor |
|---|---|
| `POSTGRES_ADMIN_PASSWORD_FILE` | `/run/secrets/postgres_admin_password` no PostgreSQL/sincronizador |
| `APP_DB_PASSWORD_FILE` | inicialização/sincronização e `spring.datasource.password` na aplicação |
| `APP_USER_PASSWORD_FILE` | `app.security.user.password` |
| `APP_ADMIN_PASSWORD_FILE` | `app.security.admin.password` |

Os nomes de usuário permanecem no ambiente do container; as senhas entram pelo filesystem de secrets.

## Primeira subida no servidor

1. Instale Docker/Compose no servidor.
2. Copie o código-fonte e crie `.env` e os quatro arquivos de segredo.
3. Mantenha `APP_BIND_ADDRESS=127.0.0.1` se um proxy HTTPS no host encaminhar para a aplicação.
4. Execute:

```powershell
docker compose config
docker compose up --build -d
docker compose ps
docker compose logs -f postgres db-credential-sync app
```

5. Confirme o healthcheck local.
6. Configure DNS, certificado e proxy reverso fora deste projeto.
7. Com HTTPS ativo, mude `APP_COOKIE_SECURE=true` e recrie `app`.

`docker compose config` resolve e imprime configuração; ele pode expor nomes/caminhos e valores não-secretos do `.env`, portanto seu output não deve ser publicado indiscriminadamente.

## Acesso remoto

Com o padrão `APP_BIND_ADDRESS=127.0.0.1`, apenas processos do próprio servidor acessam a porta publicada. Essa é a configuração adequada para um proxy reverso no host:

```text
cliente HTTPS → proxy:443 → 127.0.0.1:8080 → container app → postgres:5432
```

Se `APP_BIND_ADDRESS=0.0.0.0`, a aplicação é publicada em todas as interfaces do servidor; nesse caso, firewall e TLS externo tornam-se essenciais. O código não oferece HTTPS embutido.

Nunca configure a aplicação containerizada para usar `127.0.0.1` como host do PostgreSQL: dentro do container, esse endereço aponta para o próprio container `app`. O hostname correto é `postgres`.

## Persistência e backup

O volume nomeado `pg_data` sobrevive a rebuild e recriação dos containers. `docker compose down` preserva o volume; `docker compose down -v` o remove e é destrutivo.

Antes de upgrades de schema, recriação de volume ou mudança de credenciais administrativas:

- gere backup lógico com `pg_dump` usando uma conta autorizada;
- copie o backup para armazenamento fora do host/volume;
- valide a restauração em ambiente separado;
- registre versão da aplicação e migrations presentes.

O projeto não contém rotina automática de backup/restore. As pastas `backup` criadas pelo bootstrap local também não recebem cópias automaticamente.

Para SQLite, faça backup consistente com a aplicação parada ou com ferramenta compatível com WAL; copiar apenas `banco.db` enquanto há escrita pode ignorar dados ainda em `banco.db-wal`.

## Atualização da aplicação Docker

1. Faça backup do PostgreSQL.
2. Revise migrations novas e variáveis.
3. Reconstrua e recrie:

```powershell
docker compose up --build -d app
docker compose ps
docker compose logs -f app
```

O Compose pode recriar dependências quando necessário. Na partida, o Flyway aplica migrations pendentes antes de o Hibernate validar o schema. O volume permanece.

## Rotação de senhas

### Senha do banco da aplicação

1. Troque o conteúdo do arquivo indicado por `APP_DB_PASSWORD_FILE`.
2. Execute:

```powershell
docker compose up -d --force-recreate db-credential-sync app
```

O sincronizador altera o papel antes da aplicação iniciar com o novo secret.

### Senhas do operador/administrador

1. Troque os arquivos indicados por `APP_USER_PASSWORD_FILE` e/ou `APP_ADMIN_PASSWORD_FILE`.
2. Garanta que continuem distintas e com 12+ caracteres.
3. Recrie a aplicação:

```powershell
docker compose up -d --force-recreate app
```

Usuários são em memória; não há migração de banco.

O Compose não implementa rotação automática da senha administrativa do PostgreSQL para um volume já inicializado.

## Diagnóstico operacional

```powershell
docker compose ps
docker compose logs --tail=200 app
docker compose logs --tail=200 postgres
docker compose logs db-credential-sync
docker compose exec app wget -qO- http://127.0.0.1:8080/actuator/health
```

Falhas comuns comprováveis pela configuração:

- aplicação não inicia: credenciais obrigatórias ausentes, iguais ou curtas;
- banco não fica saudável: papel/banco não criados no volume ou secret inválido;
- `ddl-auto: validate` falha: migration PostgreSQL e entidades divergiram;
- login funciona por HTTP, mas cookie falha atrás de HTTPS: proxy/cookie `Secure` configurados de forma incompatível;
- UI sem gráficos/ícones/calendário: navegador sem acesso aos CDNs usados pelos templates;
- desktop sem abrir em servidor: ambiente headless faz o launcher iniciar somente o backend, comportamento esperado.

## Produção

O Compose fornece aplicação, banco, isolamento básico, secrets e healthchecks. O repositório não fornece os seguintes componentes de produção:

- proxy reverso e certificados TLS;
- política de backup e retenção;
- coleta/rotação centralizada de logs;
- monitoramento além do health endpoint;
- orquestrador, alta disponibilidade ou replicação;
- armazenamento externo de secrets;
- limites explícitos de CPU/memória.

Esses itens devem ser tratados pela plataforma operacional sem alterar a conexão interna `app → postgres` documentada acima.

