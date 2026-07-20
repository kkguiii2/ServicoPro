package com.empresa.controleservico.service;

import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.repository.ChamadoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelatorioExcelServiceTest {

    @Test
    void geraNomesDePlanilhaValidosEUnicos() throws Exception {
        ChamadoRepository chamadoRepository = mock(ChamadoRepository.class);
        PrestadorRepository prestadorRepository = mock(PrestadorRepository.class);
        RelatorioExcelService service = new RelatorioExcelService(chamadoRepository, prestadorRepository);
        String nomeLongo = "Prestador/Com:Nome*Muito?Longo[Unidade]";

        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(
            Prestador.builder().id(1L).nome(nomeLongo).build()
        ));
        when(prestadorRepository.findById(2L)).thenReturn(Optional.of(
            Prestador.builder().id(2L).nome(nomeLongo.toLowerCase()).build()
        ));
        when(chamadoRepository.findParaRelatorio(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());

        byte[] bytes = service.gerarRelatorioCompleto(
            List.of(1L, 2L),
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2027, 1, 1, 0, 0)
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).hasSizeLessThanOrEqualTo(31);
            assertThat(workbook.getSheetName(1)).hasSizeLessThanOrEqualTo(31);
            assertThat(workbook.getSheetName(0)).isNotEqualToIgnoringCase(workbook.getSheetName(1));
            assertThat(workbook.getSheetName(0)).doesNotContain("/", "\\", "?", "*", "[", "]", ":");
        }
    }
}
