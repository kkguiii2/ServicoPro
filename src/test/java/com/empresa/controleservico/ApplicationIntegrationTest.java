package com.empresa.controleservico;

import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.entity.Equipamento;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.enums.StatusChamado;
import com.empresa.controleservico.repository.ChamadoRepository;
import com.empresa.controleservico.repository.EquipamentoRepository;
import com.empresa.controleservico.repository.MotivoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import com.empresa.controleservico.repository.SetorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired private ChamadoRepository chamadoRepository;
    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private PrestadorRepository prestadorRepository;
    @Autowired private SetorRepository setorRepository;
    @Autowired private MotivoRepository motivoRepository;

    @MockBean
    private DesktopLauncher desktopLauncher;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("APP_DB_URL", () -> "jdbc:sqlite:file:integration-test?mode=memory&cache=shared&foreign_keys=on");
        registry.add("app.security.user.username", () -> "operador");
        registry.add("app.security.user.password", () -> "senha-operador-segura");
        registry.add("app.security.admin.username", () -> "administrador");
        registry.add("app.security.admin.password", () -> "senha-administrador-segura");
    }

    @BeforeEach
    void criarChamadosParaPaginacao() {
        if (chamadoRepository.count() > 0) {
            return;
        }
        var prestador = prestadorRepository.findByAtivoTrueOrderByNomeAsc().get(0);
        var setor = setorRepository.findByAtivoTrueOrderByNomeAsc().get(0);
        var motivo = motivoRepository.findAllByOrderByDescricaoAsc().get(0);
        IntStream.rangeClosed(1, 21)
            .mapToObj(numero -> Chamado.builder()
                .numeroCh("CH-" + numero)
                .prestador(prestador)
                .setor(setor)
                .motivo(motivo)
                .descricaoAtendimento("Atendimento " + numero)
                .dataAbertura(LocalDateTime.of(2026, 7, 1, 8, 0).plusMinutes(numero))
                .status(StatusChamado.ABERTO)
                .build())
            .forEach(chamadoRepository::save);
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void iniciaComSqliteEProcessaDashboardThymeleaf() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard/index"));
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void processaPaginacaoComMaisDeUmaPagina() throws Exception {
        mockMvc.perform(get("/chamados").param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(view().name("chamados/lista"))
            .andExpect(content().string(containsString("<span class=\"pagination__link disabled\">◀ Anterior</span>")))
            .andExpect(content().string(containsString("class=\"pagination__link\">Próxima ▶</a>")));

        mockMvc.perform(get("/chamados").param("size", "20").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"pagination__link\">◀ Anterior</a>")))
            .andExpect(content().string(containsString("<span class=\"pagination__link disabled\">Próxima ▶</span>")));
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void apiRetornaProblemDetailParaParametroInvalido() throws Exception {
        mockMvc.perform(get("/api/equipamentos").param("prestadorId", "inválido"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.detail").value("Parâmetros inválidos para a requisição."));
    }

    @Test
    void apiRetornaProblemDetailQuandoNaoAutenticada() throws Exception {
        mockMvc.perform(get("/api/equipamentos").param("prestadorId", "1"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.detail").value("Autenticação necessária para acessar este recurso."));
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void relatorioExigeDatasERetornaDownloadComoAnexo() throws Exception {
        mockMvc.perform(get("/relatorios"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"dataInicio\" class=\"form-control\" required")))
            .andExpect(content().string(containsString("name=\"dataFim\" class=\"form-control\" required")));

        mockMvc.perform(get("/relatorios/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error/general"));

        mockMvc.perform(get("/relatorios/excel")
                .param("dataInicio", "2026-07-01")
                .param("dataFim", "2026-07-31"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", startsWith("attachment;")));
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void permiteEditarChamadoComAssociacoesInativas() throws Exception {
        Prestador prestador = prestadorRepository.save(Prestador.builder()
            .nome("Prestador inativo")
            .ativo(false)
            .build());
        Equipamento equipamento = equipamentoRepository.save(Equipamento.builder()
            .nome("Equipamento inativo")
            .prestador(prestador)
            .ativo(false)
            .build());
        Chamado chamado = chamadoRepository.save(Chamado.builder()
            .prestador(prestador)
            .equipamento(equipamento)
            .setor(setorRepository.findByAtivoTrueOrderByNomeAsc().get(0))
            .motivo(motivoRepository.findAllByOrderByDescricaoAsc().get(0))
            .descricaoAtendimento("Atendimento legado")
            .dataAbertura(LocalDateTime.of(2026, 7, 1, 8, 0))
            .status(StatusChamado.ABERTO)
            .build());

        mockMvc.perform(get("/chamados/{id}/editar", chamado.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Prestador inativo")))
            .andExpect(content().string(containsString("Equipamento inativo")));
    }
}
