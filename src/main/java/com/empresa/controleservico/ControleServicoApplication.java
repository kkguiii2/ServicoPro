package com.empresa.controleservico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Ponto de entrada da aplicação Spring Boot.
 * Configura UTF-8, habilita o modo gráfico necessário ao perfil local e registra
 * a preparação antecipada do banco SQLite.
 */
@SpringBootApplication
@EnableScheduling
public class ControleServicoApplication {

    /**
     * Inicializa o contexto Spring e o servidor HTTP embutido.
     *
     * @param args argumentos repassados ao Spring Boot
     */
    public static void main(String[] args) {
        // Garante UTF-8 como charset padrão da JVM em qualquer plataforma (Windows incluso).
        // Deve ser a primeira coisa executada, antes que Spring ou qualquer I/O seja feito.
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("stdout.encoding", "UTF-8");
        System.setProperty("stderr.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        try {
            Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
            defaultCharset.setAccessible(true);
            defaultCharset.set(null, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // Java 17+ pode bloquear via strong encapsulation; as propriedades acima já cobrem.
        }

        SpringApplication application = new SpringApplication(ControleServicoApplication.class);
        application.setHeadless(false);
        application.addListeners(new LocalDatabaseBootstrapper());
        application.run(args);
    }
}
