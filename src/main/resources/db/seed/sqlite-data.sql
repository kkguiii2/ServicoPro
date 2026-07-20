INSERT OR IGNORE INTO prestadores (nome, descricao, ativo) VALUES
  ('AMAZONCOPY', 'Serviços de impressão e cópia', TRUE),
  ('BRASILINE', 'Serviços de TI e infraestrutura', TRUE),
  ('NET2PHONE', 'Serviços de telefonia VoIP', TRUE);

INSERT OR IGNORE INTO setores (nome, ativo) VALUES
  ('RH', TRUE), ('Materiais', TRUE), ('Manutenção', TRUE),
  ('Adm. Industrial', TRUE), ('Adm. Qualidade', TRUE), ('Produção Extrusão', TRUE),
  ('TI', TRUE), ('Financeiro', TRUE), ('Comercial', TRUE), ('Diretoria', TRUE);

INSERT OR IGNORE INTO motivos (descricao) VALUES
  ('Manchas'), ('Papel engatado'), ('Toner vazio'), ('Falhas em linhas'),
  ('Configuração de rede'), ('Troca de peça'), ('Troca de equipamento'),
  ('Bandeja engatando papel'), ('Impressão não saindo'), ('Sem sinal / Sem conexão'),
  ('Lentidão no sistema'), ('Problema de áudio/vídeo'), ('Ramal sem funcionamento'),
  ('Configuração de ramal'), ('Troca de headset'), ('Manutenção preventiva'),
  ('Atualização de firmware'), ('Defeito mecânico'), ('Outro');

INSERT OR IGNORE INTO equipamentos (nome, modelo, prestador_id, ativo) VALUES
  ('Impressora HP Recepção', 'HP LaserJet Pro M404n', (SELECT id FROM prestadores WHERE nome = 'AMAZONCOPY'), TRUE),
  ('Impressora Canon Adm', 'Canon imageRUNNER 2206N', (SELECT id FROM prestadores WHERE nome = 'AMAZONCOPY'), TRUE),
  ('Multifuncional Xerox TI', 'Xerox WorkCentre 3335', (SELECT id FROM prestadores WHERE nome = 'AMAZONCOPY'), TRUE),
  ('Switch Core', 'Cisco SG350-28', (SELECT id FROM prestadores WHERE nome = 'BRASILINE'), TRUE),
  ('Roteador Borda', 'MikroTik RB4011', (SELECT id FROM prestadores WHERE nome = 'BRASILINE'), TRUE),
  ('Servidor de Arquivos', 'Dell PowerEdge R340', (SELECT id FROM prestadores WHERE nome = 'BRASILINE'), TRUE),
  ('Central PABX', 'Net2Phone UCaaS Hub', (SELECT id FROM prestadores WHERE nome = 'NET2PHONE'), TRUE),
  ('Ramal IP Recepção', 'Yealink T42S', (SELECT id FROM prestadores WHERE nome = 'NET2PHONE'), TRUE),
  ('Ramal IP Diretoria', 'Yealink T58A', (SELECT id FROM prestadores WHERE nome = 'NET2PHONE'), TRUE);
