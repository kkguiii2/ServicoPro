package com.empresa.controleservico.service;

import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.enums.ConceitoAvaliacao;
import com.empresa.controleservico.repository.ChamadoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Gera relatórios XLSX de chamados, organizando cada prestador em uma planilha própria.
 */
@Service
@RequiredArgsConstructor
public class RelatorioExcelService {

    private final ChamadoRepository   chamadoRepository;
    private final PrestadorRepository  prestadorRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Produz uma pasta de trabalho com os chamados do intervalo semiaberto informado.
     * Identificadores repetidos são processados uma única vez e nomes de abas são
     * normalizados conforme as restrições do formato XLSX.
     *
     * @param prestadorIds prestadores que terão uma aba no arquivo
     * @param dataInicio início inclusivo do período
     * @param dataFim fim exclusivo do período
     * @return conteúdo binário do arquivo XLSX
     * @throws IOException quando a escrita da pasta de trabalho falha
     */
    public byte[] gerarRelatorioCompleto(
            List<Long> prestadorIds,
            LocalDateTime dataInicio,
            LocalDateTime dataFim) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Set<String> nomesUtilizados = new HashSet<>();
            for (Long prestadorId : prestadorIds.stream().distinct().toList()) {
                Prestador prestador = prestadorRepository.findById(prestadorId)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Prestador #" + prestadorId + " não encontrado"));
                List<Chamado> chamados = chamadoRepository
                    .findParaRelatorio(
                        prestadorId, dataInicio, dataFim);

                Sheet sheet = wb.createSheet(criarNomePlanilha(prestador.getNome(), nomesUtilizados));
                criarCabecalho(wb, sheet, prestador.getNome());
                preencherDados(wb, sheet, chamados);
                ajustarColunas(sheet);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private String criarNomePlanilha(String nomePrestador, Set<String> nomesUtilizados) {
        String base = WorkbookUtil.createSafeSheetName(nomePrestador, '_');
        if (base.isBlank()) {
            base = "Prestador";
        }
        String candidato = base;
        int sequencia = 2;
        while (!nomesUtilizados.add(candidato.toLowerCase(Locale.ROOT))) {
            String sufixo = " (" + sequencia++ + ")";
            candidato = base.substring(0, Math.min(base.length(), 31 - sufixo.length())) + sufixo;
        }
        return candidato;
    }

    private void criarCabecalho(XSSFWorkbook wb, Sheet sheet, String nomePrestador) {
        // Linha 1: Título mesclado
        Row row1 = sheet.createRow(0);
        row1.setHeightInPoints(24);
        Cell titulo = row1.createCell(1);
        titulo.setCellValue("CONTROLE DE ACOMPANHAMENTO DE PRESTADOR DE SERVIÇO — " + nomePrestador);
        titulo.setCellStyle(estiloCabecalhoTitulo(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 16));

        // Linha 2: Seções CHAMADO e AVALIAÇÃO
        Row row2 = sheet.createRow(1);
        row2.setHeightInPoints(20);
        Cell chamadoLabel = row2.createCell(1);
        chamadoLabel.setCellValue("CHAMADO");
        chamadoLabel.setCellStyle(estiloCabecalhoSecao(wb, new byte[]{(byte) 0x4F, (byte) 0x81, (byte) 0xBD}));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 10));

        Cell avalLabel = row2.createCell(11);
        avalLabel.setCellValue("AVALIAÇÃO");
        avalLabel.setCellStyle(estiloCabecalhoSecao(wb, new byte[]{(byte) 0x4F, (byte) 0x81, (byte) 0xBD}));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 11, 16));

        // Linha 3-4: Subcolunas
        String[] colsChamado = {
            "Nº CH", "Data Abertura", "Data Fechamento",
            "Equipamento", "Setor/Solicitante", "Motivo",
            "Descrição do Atendimento", "Tempo Atend.", "Status",
            "Ocorrências"
        };
        String[] colsConceito = {"Excelente", "Muito Bom", "Bom", "Regular", "Insatisfatório"};

        Row row3 = sheet.createRow(2);
        Row row4 = sheet.createRow(3);
        row3.setHeightInPoints(30);
        row4.setHeightInPoints(18);

        CellStyle colStyle = estiloColuna(wb);
        for (int i = 0; i < colsChamado.length; i++) {
            Cell c = row3.createCell(1 + i);
            c.setCellValue(colsChamado[i]);
            c.setCellStyle(colStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 1 + i, 1 + i));
        }

        Cell conceitoHeader = row3.createCell(11);
        conceitoHeader.setCellValue("Conceito");
        conceitoHeader.setCellStyle(colStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 11, 15));

        for (int i = 0; i < colsConceito.length; i++) {
            Cell c = row4.createCell(11 + i);
            c.setCellValue(colsConceito[i]);
            c.setCellStyle(colStyle);
        }

        Cell obsHeader = row3.createCell(16);
        obsHeader.setCellValue("Observação");
        obsHeader.setCellStyle(colStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 3, 16, 16));
    }

    private void preencherDados(XSSFWorkbook wb, Sheet sheet, List<Chamado> chamados) {
        int rowNum = 4;
        CellStyle dataCellStyle = estiloLinha(wb);
        CellStyle xStyle        = estiloX(wb);

        for (Chamado c : chamados) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(16);

            createCell(row, 1,  c.getNumeroCh() != null ? c.getNumeroCh() : "", dataCellStyle);
            createCell(row, 2,  c.getDataAbertura() != null ? c.getDataAbertura().format(DTF) : "", dataCellStyle);
            createCell(row, 3,  c.getDataFechamento() != null ? c.getDataFechamento().format(DTF) : "", dataCellStyle);
            createCell(row, 4,  c.getEquipamento() != null ? c.getEquipamento().getNome() : "-", dataCellStyle);
            createCell(row, 5,  c.getSetor() != null ? c.getSetor().getNome() : "", dataCellStyle);
            createCell(row, 6,  c.getMotivo() != null ? c.getMotivo().getDescricao() : "", dataCellStyle);
            createCell(row, 7,  c.getDescricaoAtendimento() != null ? c.getDescricaoAtendimento() : "", dataCellStyle);
            createCell(row, 8,  c.tempoFormatado(), dataCellStyle);
            createCell(row, 9,  c.getStatus() != null ? c.getStatus().getDescricao() : "", dataCellStyle);
            createCell(row, 10, String.valueOf(c.getOcorrenciasMesmoServico() != null ? c.getOcorrenciasMesmoServico() : 0), dataCellStyle);

            // Avaliação: X na coluna correta
            int[] conceitoCols = {11, 12, 13, 14, 15};
            for (int col : conceitoCols) createCell(row, col, "", dataCellStyle);

            if (c.getConceito() != null) {
                int colConceito = switch (c.getConceito()) {
                    case EXCELENTE      -> 11;
                    case MUITO_BOM      -> 12;
                    case BOM            -> 13;
                    case REGULAR        -> 14;
                    case INSATISFATORIO -> 15;
                };
                row.getCell(colConceito).setCellValue("X");
                row.getCell(colConceito).setCellStyle(xStyle);
            }

            createCell(row, 16, c.getObservacao() != null ? c.getObservacao() : "", dataCellStyle);
        }

        // Linha de total
        if (!chamados.isEmpty()) {
            Row totalRow = sheet.createRow(rowNum);
            Cell totalLabel = totalRow.createCell(1);
            totalLabel.setCellValue("Total: " + chamados.size() + " chamados");
            totalLabel.setCellStyle(estiloTotal(wb));
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, 5));
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void ajustarColunas(Sheet sheet) {
        int[] larguras = {0, 3000, 5000, 5000, 6000, 4000, 6000, 10000, 3500, 3500, 3500, 3000, 3000, 3000, 3000, 3500, 6000};
        for (int i = 1; i <= 16; i++) {
            sheet.setColumnWidth(i, larguras[i]);
        }
    }

    // ── Estilos ──────────────────────────────────────────────────────────────

    private CellStyle estiloCabecalhoTitulo(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x1E, (byte)0x3A, (byte)0x5F}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle estiloCabecalhoSecao(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle estiloColuna(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xD6, (byte)0xE4, (byte)0xF0}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
    }

    private CellStyle estiloLinha(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFont(wb.createFont());
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);
        setBorders(style);
        return style;
    }

    private CellStyle estiloX(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte)0x1E, (byte)0x6B, (byte)0x3A}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle estiloTotal(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xEE, (byte)0xF2, (byte)0xFF}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
