package com.empresa.controleservico.controller;

import com.empresa.controleservico.repository.ChamadoHistoricoRepository;
import com.empresa.controleservico.repository.MotivoRepository;
import com.empresa.controleservico.repository.SetorRepository;
import com.empresa.controleservico.service.ChamadoService;
import com.empresa.controleservico.service.EquipamentoService;
import com.empresa.controleservico.service.PrestadorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChamadoControllerTest {

    private ChamadoService chamadoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chamadoService = mock(ChamadoService.class);
        PrestadorService prestadorService = mock(PrestadorService.class);
        EquipamentoService equipamentoService = mock(EquipamentoService.class);
        ChamadoHistoricoRepository historicoRepository = mock(ChamadoHistoricoRepository.class);
        SetorRepository setorRepository = mock(SetorRepository.class);
        MotivoRepository motivoRepository = mock(MotivoRepository.class);
        when(chamadoService.listar(
            null, null, null, null, null,
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 8, 1, 0, 0),
            null, null, 0, 20, "dataAbertura", "desc"
        )).thenReturn(Page.empty());

        mockMvc = MockMvcBuilders.standaloneSetup(new ChamadoController(
            chamadoService,
            prestadorService,
            equipamentoService,
            historicoRepository,
            setorRepository,
            motivoRepository
        )).build();
    }

    @Test
    void converteDatasDoFiltroParaIntervaloSemiaberto() throws Exception {
        mockMvc.perform(get("/chamados")
                .param("dataInicio", "2026-07-01")
                .param("dataFim", "2026-07-31"))
            .andExpect(status().isOk());

        verify(chamadoService).listar(
            eq(null), eq(null), eq(null), eq(null), eq(null),
            eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
            eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
            eq(null), eq(null), eq(0), eq(20), eq("dataAbertura"), eq("desc")
        );
    }
}
