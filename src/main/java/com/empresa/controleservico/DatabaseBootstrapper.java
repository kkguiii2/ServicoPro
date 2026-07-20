package com.empresa.controleservico;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Prepara os diretórios locais, resolve o arquivo SQLite e habilita o modo WAL
 * antes da criação do {@code DataSource} pelo Spring.
 */
public class DatabaseBootstrapper {

    /**
     * Resolve a localização do banco local e publica o caminho na propriedade
     * de sistema {@code APP_DB_PATH}.
     */
    public static void bootstrap() {
        System.out.println("Verificando pastas do sistema e inicializando SQLite...");

        String configuredPath = System.getProperty("APP_DB_PATH");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv("APP_DB_PATH");
        }

        if (configuredPath != null && !configuredPath.isBlank()) {
            File dbFile = new File(configuredPath).getAbsoluteFile();
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Não foi possível criar o diretório do banco: " + parent);
            }
            configurarBanco(dbFile);
            return;
        }

        // Determina o diretório base baseado no ProgramData ou fallback local
        String baseDir = System.getenv("ProgramData");
        if (baseDir == null || baseDir.trim().isEmpty()) {
            baseDir = ".";
            System.out.println("ProgramData não detectado. Usando diretório de execução local.");
        } else {
            baseDir = baseDir + File.separator + "ControleServico";
            System.out.println("ProgramData detectado. Pasta base definida em: " + baseDir);
        }
        
        File baseFolder = new File(baseDir);
        
        // Pastas requisitadas dentro do diretório base
        String[] pastas = {"data", "backup", "logs", "config"};
        
        for (String pasta : pastas) {
            File dir = new File(baseFolder, pasta);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    System.out.println("Diretório '" + dir.getAbsolutePath() + "' criado com sucesso.");
                } else {
                    System.err.println("Falha ao criar diretório '" + dir.getAbsolutePath() + "'.");
                }
            }
        }

        configurarBanco(new File(baseFolder, "data" + File.separator + "banco.db"));
    }

    private static void configurarBanco(File dbFile) {
        String dbPath = dbFile.getAbsolutePath();

        // Configura a propriedade de sistema para ser lida pelo Spring Boot no application.yml
        System.setProperty("APP_DB_PATH", dbPath.replace("\\", "/"));
        System.out.println("SQLite: Banco de dados configurado em: " + dbPath);

        // Habilita o modo WAL para o SQLite
        try {
            // A própria tentativa de conexão cria o banco se não existir
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL;");
                    System.out.println("SQLite: Modo WAL habilitado com sucesso.");
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao inicializar configurações do SQLite: " + e.getMessage());
        }
    }
}
