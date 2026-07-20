package com.empresa.controleservico;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Inicia a experiência desktop do perfil local em uma janela Swing com navegador JCEF.
 * O backend continua disponível sem janela quando o ambiente é headless.
 */
@Component
@Profile("local")
public class DesktopLauncher implements CommandLineRunner {

    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5",
            "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5",
            "LPT6", "LPT7", "LPT8", "LPT9");

    @Value("${server.port:8080}")
    private int port;

    /**
     * Inicializa o runtime JCEF fora da EDT e abre a aplicação local após o Spring iniciar.
     *
     * @param args argumentos da linha de comando
     */
    @Override
    public void run(String... args) {
        // Se estiver num servidor CI/CD ou sem tela, aborta.
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Ambiente headless detectado. Iniciando apenas o backend.");
            return;
        }

        System.out.println("Iniciando interface Desktop com JCEF...");

        // INICIALIZA O AWT ANTES DE TUDO: Previne deadlocks severos no Windows/MacOS ao integrar Swing com CEF
        java.awt.Toolkit.getDefaultToolkit();

        System.out.println("Inicializando JCEF (isso pode demorar na primeira vez para baixar os binários)...");

        // Executar a inicialização em uma Thread separada para não travar a Thread principal do Spring Boot (Tomcat)
        new Thread(() -> {
            try {
                // A inicialização pesada (downloads e extração) ocorre FORA da Thread da UI (EDT)
                CefAppBuilder builder = new CefAppBuilder();
                File jcefDir = resolveJcefDirectory();
                builder.setInstallDir(jcefDir);
                builder.setSkipInstallation(isBundledJcef(jcefDir.toPath()));
                builder.getCefSettings().windowless_rendering_enabled = false;

                CefApp cefApp = builder.build();

            // Depois que o core do CEF estiver carregado, criamos a janela na thread do Swing
            SwingUtilities.invokeLater(() -> {
                try {
                    CefClient client = cefApp.createClient();
                    
                    // Adiciona um handler de download para evitar crashs do JCEF no Windows (Save As nativo)
                    client.addDownloadHandler(new org.cef.handler.CefDownloadHandlerAdapter() {
                        @Override
                        public boolean onBeforeDownload(CefBrowser browser, org.cef.callback.CefDownloadItem downloadItem,
                                                     String suggestedName, org.cef.callback.CefBeforeDownloadCallback callback) {
                            // Salva automaticamente na pasta Downloads do usuário
                            try {
                                Path downloadPath = createDownloadPath(suggestedName);
                                System.out.println("Baixando arquivo para: " + downloadPath);
                                callback.Continue(downloadPath.toString(), false);
                                return true;
                            } catch (IOException ex) {
                                showError("Não foi possível preparar o download.", ex);
                                return false;
                            }
                        }
                        
                        @Override
                        public void onDownloadUpdated(CefBrowser browser, org.cef.callback.CefDownloadItem downloadItem,
                                                      org.cef.callback.CefDownloadItemCallback callback) {
                            if (downloadItem.isComplete()) {
                                System.out.println("Download concluído: " + downloadItem.getFullPath());
                            }
                        }
                    });

                    String startUrl = "http://localhost:" + port + "/";
                    CefBrowser browser = client.createBrowser(startUrl, false, false);
                    java.awt.Component browserUI = browser.getUIComponent();
                    iniciarJanela(browserUI);
                } catch (Exception ex) {
                    showFatalError("Falha ao abrir a interface do aplicativo.", ex);
                }
            });

            } catch (Exception e) {
                showFatalError("Falha ao inicializar o Chromium Embedded Framework (JCEF).", e);
            }
        }, "JCEF-Init-Thread").start();
    }

    private File resolveJcefDirectory() throws IOException {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path bundled = Path.of(appPath).toAbsolutePath().getParent()
                    .resolve("app").resolve("jcef-runtime");
            if (isBundledJcef(bundled)) {
                return bundled.toFile();
            }
        }

        Path userDirectory = Path.of(System.getProperty("user.home"), ".jcef_controle_servico");
        Files.createDirectories(userDirectory);
        return userDirectory.toFile();
    }

    private boolean isBundledJcef(Path directory) {
        return Files.isRegularFile(directory.resolve("build_meta.json"))
                && Files.isRegularFile(directory.resolve("libcef.dll"))
                && Files.isRegularFile(directory.resolve("jcef.dll"))
                && Files.isRegularFile(directory.resolve("jcef_helper.exe"))
                && Files.isRegularFile(directory.resolve("icudtl.dat"))
                && Files.isDirectory(directory.resolve("locales"));
    }

    private synchronized Path createDownloadPath(String suggestedName) throws IOException {
        Path downloadDirectory = Path.of(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloadDirectory);

        String safeName = suggestedName == null ? "download" : suggestedName
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("[. ]+$", "");
        if (safeName.isBlank()) {
            safeName = "download";
        }
        String deviceName = safeName.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
        if (WINDOWS_RESERVED_NAMES.contains(deviceName)) {
            safeName = "_" + safeName;
        }

        Path candidate = downloadDirectory.resolve(safeName);
        String baseName = safeName;
        String extension = "";
        int extensionIndex = safeName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = safeName.substring(0, extensionIndex);
            extension = safeName.substring(extensionIndex);
        }
        for (int suffix = 1; Files.exists(candidate); suffix++) {
            candidate = downloadDirectory.resolve(baseName + " (" + suffix + ")" + extension);
        }
        return candidate;
    }

    private void showError(String message, Exception exception) {
        exception.printStackTrace();
        JOptionPane.showMessageDialog(null, message + "\n" + exception.getMessage(),
                "Controle de Serviço", JOptionPane.ERROR_MESSAGE);
    }

    private void showFatalError(String message, Exception exception) {
        Runnable closeApplication = () -> {
            showError(message, exception);
            System.exit(1);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            closeApplication.run();
        } else {
            SwingUtilities.invokeLater(closeApplication);
        }
    }

    private void iniciarJanela(java.awt.Component browserUI) {

        // Cria e configura o JFrame (janela nativa do SO)
        JFrame frame = new JFrame("Controle de Serviço - Gestão TI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(browserUI, BorderLayout.CENTER);
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null); // Centraliza na tela
        
        // Usa o mesmo PNG incorporado ao JAR durante a geração do instalador.
        try (InputStream iconStream = DesktopLauncher.class.getResourceAsStream("/setting.png")) {
            if (iconStream != null) {
                frame.setIconImage(ImageIO.read(iconStream));
            } else {
                System.err.println("Ícone setting.png não encontrado no pacote.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Adiciona um listener para encerrar o Spring Boot quando a janela for fechada
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                CefApp.getInstance().dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }
}
