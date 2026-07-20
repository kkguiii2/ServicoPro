# Controle de Serviço

## Execução local

O profile padrão `local` usa SQLite, limita o servidor a `127.0.0.1` e exige duas contas distintas.

Defina antes de iniciar:

```powershell
$env:APP_USER_USERNAME = "operador"
$env:APP_USER_PASSWORD = "uma-senha-com-12-caracteres"
$env:APP_ADMIN_USERNAME = "administrador"
$env:APP_ADMIN_PASSWORD = "outra-senha-com-12-caracteres"
mvn spring-boot:run
```

O script `instalar-como-administrador.bat` valida o checksum, instala o MSI com elevação e, após o sucesso, solicita as credenciais no contexto do usuário atual. As credenciais são persistidas como variáveis de ambiente do usuário.

## Geração do instalador Windows

Execute `gerar-instalador.bat` com dois cliques. Também é possível usar “Executar com PowerShell” em `gerar-instalador.ps1`; nesse caso, a janela aguarda ENTER antes de fechar. O script:

1. valida Maven, JDK 17+, `setting.ico` e `setting.png`;
2. baixa e valida o WiX 3.14.1 automaticamente quando ele não estiver instalado;
3. executa todos os testes, gera e publica `ControleServico-<versão>.jar` na pasta do projeto;
4. baixa e incorpora o runtime nativo do JCEF para a aplicação funcionar sem internet;
5. incorpora `setting.png` à aplicação e usa `setting.ico` no executável e atalhos;
6. usa exatamente o novo JAR publicado para criar `ControleServico-<versão>.msi` e seu checksum `.sha256` na pasta do projeto.

O WiX e o runtime JCEF baixados ficam em `.tools` para reutilização nas próximas execuções. O MSI intermediário fica em `dist/installer`.

## Execução com Docker

1. Copie `.env.example` para `.env` e ajuste usuários, porta e caminhos.
2. Crie os quatro arquivos de segredo indicados, cada um contendo somente o respectivo valor.
3. Use senhas diferentes, aleatórias e com pelo menos 12 caracteres.
4. Execute `docker compose up --build`.

Se já existir um volume `pg_data` criado pela configuração antiga, faça backup e recrie o volume para que o usuário limitado e as migrations sejam inicializados corretamente.

Para rotacionar a senha do banco, altere `APP_DB_PASSWORD_FILE` e execute `docker compose up -d --force-recreate db-credential-sync app`. O serviço de sincronização atualiza o usuário limitado antes de iniciar a aplicação.

O PostgreSQL não publica porta no host. A aplicação publica `8080` somente em `127.0.0.1` por padrão; use um proxy HTTPS para exposição em rede e configure `APP_COOKIE_SECURE=true`.
