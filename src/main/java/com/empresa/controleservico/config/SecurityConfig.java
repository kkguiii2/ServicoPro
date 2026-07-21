package com.empresa.controleservico.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import java.nio.charset.StandardCharsets;

/**
 * Define autenticação por formulário, autorização por papéis, tratamento de API
 * e usuários em memória carregados de configuração externa.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    /**
     * Monta a cadeia de filtros que separa recursos públicos, operações de usuário
     * e operações administrativas, mantendo CSRF e login por formulário ativos.
     * Requisições anônimas à API recebem Problem Details; páginas MVC seguem para o login.
     *
     * @param http configurador de segurança HTTP fornecido pelo Spring
     * @param objectMapper serializador usado nas respostas de erro da API
     * @return cadeia de filtros pronta para registro no contexto
     * @throws Exception quando o Spring Security não consegue construir a cadeia
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        AntPathRequestMatcher apiRequest = new AntPathRequestMatcher("/api/**");
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/access-denied", "/error", "/css/**", "/favicon.ico", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/chamados/*/excluir").hasRole("ADMIN")
                .requestMatchers("/prestadores/**", "/equipamentos/**", "/configuracoes/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", false)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor((request, response, exception) -> {
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED, "Autenticação necessária para acessar este recurso.");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), problem);
                }, apiRequest)
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new NegatedRequestMatcher(apiRequest))
                .accessDeniedPage("/access-denied"))
            .build();
    }

    /**
     * Cria o encoder delegante usado para codificar as senhas recebidas da configuração.
     *
     * @return encoder com identificação explícita do algoritmo no hash
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Cria as contas em memória a cada inicialização, atribuindo USER ao operador
     * e USER/ADMIN ao administrador.
     *
     * @param properties credenciais já validadas e carregadas de fonte externa
     * @param encoder encoder usado antes de armazenar as senhas em memória
     * @return serviço de usuários com as duas contas configuradas
     */
    @Bean
    UserDetailsService userDetailsService(SecurityProperties properties, PasswordEncoder encoder) {
        SecurityProperties.Account user = properties.user();
        SecurityProperties.Account admin = properties.admin();
        return new InMemoryUserDetailsManager(
            User.withUsername(user.username())
                .password(encoder.encode(user.password()))
                .roles("USER")
                .build(),
            User.withUsername(admin.username())
                .password(encoder.encode(admin.password()))
                .roles("USER", "ADMIN")
                .build()
        );
    }
}
