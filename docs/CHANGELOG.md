# Changelog

Este arquivo segue as categorias do Keep a Changelog, adaptadas ao histórico verificável disponível.

O workspace recebido não contém diretório `.git`, commits, tags ou notas de versão. Por isso, não é possível atribuir datas, autores ou ordem histórica às correções anteriores sem fazer suposições. A seção 1.0.0 registra somente correções comprovadas pelo código e pelos testes existentes; a seção de 19/07/2026 registra o trabalho desta revisão documental.

## [Não lançado] — 2026-07-19

### Adicionado

- documentação técnica completa em `docs/` para visão geral, arquitetura, banco, endpoints, regras, segurança, implantação e manutenção;
- diagramas Mermaid de arquitetura, requisições, módulos, fluxo de chamado e modelo ER;
- inventário de variáveis de ambiente, profiles, secrets e modos de execução;
- documentação explícita sobre execução local versus servidor e endereçamento `127.0.0.1` versus DNS interno `postgres`;
- JavaDoc complementar em pontos públicos e de infraestrutura que possuíam contrato incompleto.

### Documentado

- divergências verificadas entre o arquivo SQLite presente e o schema PostgreSQL/mapeamento atual;
- limitações comprovadas de segurança, auditoria, dependências CDN e operação;
- fluxo de build JAR/MSI, checksum, JCEF offline e Compose;
- necessidade de rebuild/restart/reinstalação conforme o modo de entrega.

### Alterado

- nenhuma regra de negócio ou funcionalidade foi alterada nesta revisão.

## [1.0.0] — data histórica não disponível

### Correções verificadas pela suíte atual

- reabertura limpa data e tempo de fechamento e registra o novo estado no histórico (`ChamadoServiceTest`);
- edição sem equipamento remove a associação anterior (`ChamadoServiceTest`);
- equipamento de outro prestador é rejeitado (`ChamadoServiceTest`);
- callback de duração calcula minutos sem alterar o status solicitado (`ChamadoServiceTest`);
- filtro de prestador é propagado às métricas do dashboard (`DashboardServiceTest`);
- nomes de planilha Excel são sanitizados, limitados e tornados únicos (`RelatorioExcelServiceTest`);
- datas finais de filtros são convertidas para intervalo semiaberto, incluindo todo o último dia (`ChamadoControllerTest`);
- usuário anônimo é enviado ao login e operador não acessa cadastro administrativo (`SecurityConfigTest`);
- POST administrativo exige CSRF (`SecurityConfigTest`);
- contas com privilégios diferentes não podem compartilhar a mesma senha (`SecurityConfigTest`);
- aplicação inicia com SQLite e processa dashboard Thymeleaf (`ApplicationIntegrationTest`);
- paginação renderiza corretamente primeira e última página (`ApplicationIntegrationTest`);
- API retorna Problem Details para parâmetro inválido e falta de autenticação (`ApplicationIntegrationTest`);
- relatório exige as duas datas e retorna download como anexo (`ApplicationIntegrationTest`);
- edição preserva associações inativas já ligadas ao chamado (`ApplicationIntegrationTest`);
- modais de cadastro iniciam fechados, preservam IDs únicos e não dependem apenas do CSS para fechar (`ModalContractTest`);
- controlador de modal evita listeners duplicados, trata backdrop/Escape/foco e continua fechando sem CSS (`modal-browser-test.mjs`);
- gerador de instalador valida que o JAR recém-compilado é exatamente o artefato enviado ao `jpackage` (`gerar-instalador.Tests.ps1`);
- MSI tem checksum SHA-256 verificado antes da instalação, e o WiX baixado é validado por hash (`instalar-como-administrador.bat`, `gerar-instalador.ps1`).

### Segurança consolidada no estado atual

- contas e senhas obrigatórias, distintas e externas ao código;
- autorização por `USER`/`ADMIN` e exclusão de chamado restrita a administrador;
- CSRF ativo, sessão invalidada no logout e cookie configurável como `Secure`;
- PostgreSQL sem porta publicada e aplicação Docker executada como usuário não-root;
- papel PostgreSQL da aplicação sem privilégios administrativos e com rotação de senha coordenada.

