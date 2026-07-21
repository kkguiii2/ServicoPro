package com.empresa.controleservico.config;

import com.empresa.controleservico.controller.PrestadorController;
import com.empresa.controleservico.service.PrestadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WebMvcTest(PrestadorController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.security.user.username=operador",
    "app.security.user.password=senha-operador-segura",
    "app.security.admin.username=administrador",
    "app.security.admin.password=senha-administrador-segura"
})
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PrestadorService prestadorService;

    @Test
    void redirecionaAnonimoParaLogin() throws Exception {
        mockMvc.perform(get("/prestadores"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void bloqueiaCadastroMestreParaOperador() throws Exception {
        mockMvc.perform(get("/prestadores"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void exigeCsrfEmOperacaoAdministrativa() throws Exception {
        mockMvc.perform(post("/prestadores/salvar").param("nome", "Novo"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/prestadores/salvar")
                .with(csrf())
                .param("nome", "Novo"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void rejeitaMesmaSenhaParaContasComPrivilegiosDiferentes() {
        var senha = "senha-compartilhada-insegura";
        assertThatThrownBy(() -> new SecurityProperties(
            new SecurityProperties.Account("operador", senha),
            new SecurityProperties.Account("administrador", senha)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("senhas");
    }
}
