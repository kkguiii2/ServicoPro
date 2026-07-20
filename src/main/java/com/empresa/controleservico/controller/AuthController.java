package com.empresa.controleservico.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Expõe as páginas relacionadas à autenticação e à negação de acesso.
 */
@Controller
public class AuthController {

    /**
     * Exibe o formulário de login ou redireciona uma sessão já autenticada.
     *
     * @param authentication autenticação atual, quando existente
     * @return nome da view de login ou redirecionamento para o dashboard
     */
    @GetMapping("/login")
    public String login(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
            ? "redirect:/dashboard"
            : "auth/login";
    }

    /**
     * Exibe a página utilizada pelo Spring Security para acessos negados.
     *
     * @return nome da view HTTP 403
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}
