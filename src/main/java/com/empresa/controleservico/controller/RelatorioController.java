package com.empresa.controleservico.controller;

import com.empresa.controleservico.service.RelatorioPdfService;
import com.empresa.controleservico.service.RelatorioExcelService;
import com.empresa.controleservico.service.PrestadorService;
import com.empresa.controleservico.entity.Prestador;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Disponibiliza a tela e os downloads de relatórios de chamados em Excel e PDF.
 */
@Controller
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioExcelService relatorioExcelService;
    private final RelatorioPdfService   relatorioPdfService;
    private final PrestadorService      prestadorService;

    /**
     * Exibe os filtros disponíveis para geração de relatórios.
     *
     * @param model modelo da view Thymeleaf
     * @return template da página de relatórios
     */
    @GetMapping
    public String pagina(Model model) {
        model.addAttribute("prestadores", prestadorService.listarAtivos());
        model.addAttribute("pageTitle",  "Relatórios");
        model.addAttribute("activePage", "relatorios");
        return "relatorios/index";
    }

    /**
     * Gera uma planilha com uma aba por prestador no intervalo informado.
     *
     * @param prestadorIds prestadores selecionados; vazio significa todos os ativos
     * @param dataInicio primeiro dia incluído
     * @param dataFim último dia incluído
     * @return arquivo XLSX como anexo HTTP
     * @throws IOException quando a planilha não pode ser produzida
     */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) List<Long> prestadorIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws IOException {

        validarPeriodo(dataInicio, dataFim);

        if (prestadorIds == null || prestadorIds.isEmpty()) {
            prestadorIds = prestadorService.listarAtivos()
                .stream().map(Prestador::getId).collect(Collectors.toList());
        }

        byte[] excel = relatorioExcelService.gerarRelatorioCompleto(
            prestadorIds,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "Controle_Servico_" +
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build());

        return ResponseEntity.ok().headers(headers).body(excel);
    }

    /**
     * Gera um PDF consolidado por prestador no intervalo informado.
     *
     * @param prestadorIds prestadores selecionados; vazio significa todos os ativos
     * @param dataInicio primeiro dia incluído
     * @param dataFim último dia incluído
     * @return arquivo PDF como anexo HTTP
     * @throws Exception quando o documento não pode ser produzido
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) List<Long> prestadorIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
            throws Exception {

        validarPeriodo(dataInicio, dataFim);

        if (prestadorIds == null || prestadorIds.isEmpty()) {
            prestadorIds = prestadorService.listarAtivos()
                .stream().map(Prestador::getId).collect(Collectors.toList());
        }

        byte[] pdf = relatorioPdfService.gerarRelatorioPdf(
            prestadorIds,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("Relatorio_Servico_" + LocalDate.now() + ".pdf", StandardCharsets.UTF_8)
            .build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial");
        }
    }
}
