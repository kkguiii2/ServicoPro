package com.empresa.controleservico;

import me.friwi.jcefmaven.CefAppBuilder;

import java.io.File;

/**
 * Utilitário de build que instala e valida o runtime nativo do JCEF incorporado ao MSI.
 */
public final class JcefRuntimeInstaller {

    private JcefRuntimeInstaller() {
    }

    /**
     * Instala o runtime no único diretório recebido e verifica os arquivos mínimos.
     *
     * @param args diretório de destino na primeira posição
     * @throws Exception quando a instalação ou validação falha
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Informe o diretório de destino do runtime JCEF.");
        }

        File installDirectory = new File(args[0]);
        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(installDirectory);
        builder.install();

        String[] requiredFiles = {
                "build_meta.json", "libcef.dll", "jcef.dll", "jcef_helper.exe", "icudtl.dat"
        };
        for (String requiredFile : requiredFiles) {
            if (!new File(installDirectory, requiredFile).isFile()) {
                throw new IllegalStateException("Arquivo ausente no runtime JCEF: " + requiredFile);
            }
        }
        if (!new File(installDirectory, "locales").isDirectory()) {
            throw new IllegalStateException("O runtime JCEF não foi instalado corretamente.");
        }
    }
}
