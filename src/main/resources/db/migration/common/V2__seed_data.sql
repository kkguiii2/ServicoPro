-- encoding: UTF-8

INSERT INTO prestadores (nome, descricao, ativo)
SELECT 'AMAZONCOPY', 'Serviços de impressão e cópia', TRUE
WHERE NOT EXISTS (SELECT 1 FROM prestadores WHERE nome = 'AMAZONCOPY');
INSERT INTO prestadores (nome, descricao, ativo)
SELECT 'BRASILINE', 'Serviços de TI e infraestrutura', TRUE
WHERE NOT EXISTS (SELECT 1 FROM prestadores WHERE nome = 'BRASILINE');
INSERT INTO prestadores (nome, descricao, ativo)
SELECT 'NET2PHONE', 'Serviços de telefonia VoIP', TRUE
WHERE NOT EXISTS (SELECT 1 FROM prestadores WHERE nome = 'NET2PHONE');


INSERT INTO setores (nome, ativo)
SELECT seed.nome, TRUE
FROM (VALUES
    ('RH'), ('Materiais'), ('Manutenção'), ('Adm. Industrial'), ('Adm. Qualidade'),
    ('Produção Extrusão'), ('TI'), ('Financeiro'), ('Comercial'), ('Diretoria')
) AS seed(nome)
WHERE NOT EXISTS (SELECT 1 FROM setores s WHERE s.nome = seed.nome);

INSERT INTO motivos (descricao)
SELECT seed.descricao
FROM (VALUES
    ('Manchas'), ('Papel engatado'), ('Toner vazio'), ('Falhas em linhas'),
    ('Configuração de rede'), ('Troca de peça'), ('Troca de equipamento'),
    ('Bandeja engatando papel'), ('Impressão não saindo'), ('Sem sinal / Sem conexão'),
    ('Lentidão no sistema'), ('Problema de áudio/vídeo'), ('Ramal sem funcionamento'),
    ('Configuração de ramal'), ('Troca de headset'), ('Manutenção preventiva'),
    ('Atualização de firmware'), ('Defeito mecânico'), ('Outro')
) AS seed(descricao)
WHERE NOT EXISTS (SELECT 1 FROM motivos m WHERE m.descricao = seed.descricao);

INSERT INTO equipamentos (nome, modelo, prestador_id, ativo)
SELECT seed.nome, seed.modelo, p.id, TRUE
FROM (VALUES
    ('Impressora HP Recepção', 'HP LaserJet Pro M404n', 'AMAZONCOPY'),
    ('Impressora Canon Adm', 'Canon imageRUNNER 2206N', 'AMAZONCOPY'),
    ('Multifuncional Xerox TI', 'Xerox WorkCentre 3335', 'AMAZONCOPY'),
    ('Switch Core', 'Cisco SG350-28', 'BRASILINE'),
    ('Roteador Borda', 'MikroTik RB4011', 'BRASILINE'),
    ('Servidor de Arquivos', 'Dell PowerEdge R340', 'BRASILINE'),
    ('Central PABX', 'Net2Phone UCaaS Hub', 'NET2PHONE'),
    ('Ramal IP Recepção', 'Yealink T42S', 'NET2PHONE'),
    ('Ramal IP Diretoria', 'Yealink T58A', 'NET2PHONE')
) AS seed(nome, modelo, prestador_nome)
JOIN prestadores p ON p.nome = seed.prestador_nome
WHERE NOT EXISTS (
    SELECT 1 FROM equipamentos e
    WHERE e.nome = seed.nome AND e.prestador_id = p.id
);
