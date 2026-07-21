package com.empresa.controleservico.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credenciais externas das contas de operador e administrador.
 * As contas devem possuir nomes e senhas distintos, e cada senha deve ter ao menos 12 caracteres.
 *
 * @param user conta com papel USER
 * @param admin conta com papéis USER e ADMIN
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotNull @Valid Account user,
        @NotNull @Valid Account admin) {

    /**
     * Valida as invariantes que envolvem simultaneamente as duas contas.
     *
     * @param user conta operacional
     * @param admin conta administrativa
     * @throws IllegalArgumentException quando nomes ou senhas são compartilhados
     */
    public SecurityProperties {
        if (user != null && admin != null && user.username() != null
                && user.username().equalsIgnoreCase(admin.username())) {
            throw new IllegalArgumentException("Os usuários operador e administrador devem ser diferentes");
        }
        if (user != null && admin != null && user.password() != null
                && user.password().equals(admin.password())) {
            throw new IllegalArgumentException("As senhas do operador e do administrador devem ser diferentes");
        }
    }

    /**
     * Credencial individual validada antes da criação do usuário em memória.
     *
     * @param username nome de login
     * @param password senha em texto recebida da configuração externa
     */
    public record Account(
            @NotBlank String username,
            @NotBlank @Size(min = 12, message = "A senha deve ter pelo menos 12 caracteres") String password) {
    }
}
