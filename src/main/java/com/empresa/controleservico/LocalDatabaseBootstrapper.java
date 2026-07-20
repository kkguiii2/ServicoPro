package com.empresa.controleservico;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Profiles;

/**
 * Listener de inicialização que executa o bootstrap do SQLite somente no perfil local.
 */
public final class LocalDatabaseBootstrapper
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    /**
     * Prepara o banco antes da construção do contexto quando o perfil {@code local} está ativo.
     *
     * @param event evento que disponibiliza o ambiente já preparado
     */
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        if (event.getEnvironment().acceptsProfiles(Profiles.of("local"))) {
            DatabaseBootstrapper.bootstrap();
        }
    }
}
