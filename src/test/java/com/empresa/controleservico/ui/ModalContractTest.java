package com.empresa.controleservico.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ModalContractTest {

    private static final Pattern HTML_ID = Pattern.compile("\\bid=\\\"([^\\\"]+)\\\"");

    @Test
    void modaisDeCadastroIniciamFechadosENaoDependemDoCssParaFechar() throws IOException {
        assertModalContract("templates/prestadores/lista.html", "modalNovoPrestador");
        assertModalContract("templates/equipamentos/lista.html", "modalNovoEquip");

        String layout = resource("templates/layout/base.html");
        String css = resource("static/css/components.css");

        assertThat(layout)
            .contains("modal.hidden = false;")
            .contains("modal.hidden = true;")
            .contains("modal.classList.add('is-open');")
            .contains("modal.classList.remove('is-open');")
            .contains("modalControllerInitialized")
            .contains("if (event.target === activeModal) closeModal(activeModal);");
        assertThat(css).contains(".modal-backdrop[hidden] { display: none !important; }");
    }

    @Test
    void paginasNaoPossuemIdsDuplicadosNemAberturaAutomatica() throws IOException {
        String layout = resource("templates/layout/base.html");

        for (String page : new String[]{
            "templates/prestadores/lista.html",
            "templates/equipamentos/lista.html"
        }) {
            String template = resource(page);
            assertUniqueIds(layout + template);
            assertThat(template)
                .doesNotContain("is-open")
                .doesNotContain("data-bs-")
                .doesNotContain("showModal(")
                .doesNotContain("Bootstrap.Modal")
                .doesNotContain("modal.open");
        }
    }

    private static void assertModalContract(String resource, String modalId) throws IOException {
        String template = resource(resource);

        assertThat(template)
            .contains("data-modal-open=\"" + modalId + "\"")
            .contains("aria-controls=\"" + modalId + "\"")
            .contains("id=\"" + modalId + "\" class=\"modal-backdrop\" data-modal hidden")
            .contains("aria-hidden=\"true\"");
        assertThat(count(template, "data-modal-close")).isEqualTo(2);
    }

    private static void assertUniqueIds(String html) {
        Matcher matcher = HTML_ID.matcher(html);
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            assertThat(ids.add(matcher.group(1)))
                .as("ID duplicado: %s", matcher.group(1))
                .isTrue();
        }
    }

    private static long count(String value, String fragment) {
        return Pattern.compile(Pattern.quote(fragment)).matcher(value).results().count();
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = ModalContractTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("Recurso no classpath: %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
