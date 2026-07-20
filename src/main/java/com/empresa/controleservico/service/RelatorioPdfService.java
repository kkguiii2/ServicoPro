package com.empresa.controleservico.service;

import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.repository.ChamadoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera relatórios PDF consolidados de chamados, agrupados visualmente por prestador.
 */
@Service
@RequiredArgsConstructor
public class RelatorioPdfService {

    private final ChamadoRepository   chamadoRepository;
    private final PrestadorRepository  prestadorRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DeviceRgb DARK_BLUE  = new DeviceRgb(0x1E, 0x3A, 0x5F);
    private static final DeviceRgb MED_BLUE   = new DeviceRgb(0x4F, 0x81, 0xBD);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(0xF0, 0xF4, 0xF8);

    /**
     * Produz um PDF em orientação paisagem para o intervalo semiaberto informado.
     *
     * @param prestadorIds prestadores incluídos no documento
     * @param dataInicio início inclusivo do período
     * @param dataFim fim exclusivo do período
     * @return conteúdo binário do PDF
     * @throws Exception quando a criação do documento falha
     */
    public byte[] gerarRelatorioPdf(
            List<Long> prestadorIds,
            LocalDateTime dataInicio,
            LocalDateTime dataFim) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer   = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document  = new Document(pdfDoc, com.itextpdf.kernel.geom.PageSize.A4.rotate());
        document.setMargins(20, 20, 20, 20);

        // Título geral
        Paragraph titulo = new Paragraph("CONTROLE DE ACOMPANHAMENTO DE PRESTADOR DE SERVIÇO")
            .setFontSize(14)
            .setBold()
            .setFontColor(DARK_BLUE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(5);
        document.add(titulo);

        String periodo = "Período: " +
            (dataInicio != null ? dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "início") +
            " a " +
            (dataFim != null ? dataFim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "hoje");
        document.add(new Paragraph(periodo)
            .setFontSize(9)
            .setFontColor(new DeviceRgb(0x6B, 0x7A, 0x8D))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(15));

        for (Long prestadorId : prestadorIds) {
            Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Prestador #" + prestadorId + " não encontrado"));
            List<Chamado> chamados = chamadoRepository
                .findParaRelatorio(
                    prestadorId, dataInicio, dataFim);

            // Cabeçalho do prestador
            document.add(new Paragraph("▶ " + prestador.getNome() +
                " — " + chamados.size() + " chamados")
                .setFontSize(11)
                .setBold()
                .setFontColor(MED_BLUE)
                .setMarginBottom(6)
                .setMarginTop(10));

            if (chamados.isEmpty()) {
                document.add(new Paragraph("Nenhum chamado no período selecionado.")
                    .setFontSize(9).setItalic().setMarginBottom(10));
                continue;
            }

            // Tabela
            Table table = new Table(UnitValue.createPercentArray(new float[]{
                4, 7, 7, 8, 6, 8, 18, 5, 6, 4, 6, 11
            })).useAllAvailableWidth();

            // Cabeçalho da tabela
            String[] headers = {
                "Nº CH", "Abertura", "Fechamento", "Equipamento",
                "Setor", "Motivo", "Descrição", "Tempo",
                "Status", "Ocorr.", "Conceito", "Observação"
            };
            for (String h : headers) {
                Cell cell = new Cell()
                    .add(new Paragraph(h).setFontSize(7).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(MED_BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(3);
                table.addHeaderCell(cell);
            }

            boolean alternateRow = false;
            for (Chamado c : chamados) {
                DeviceRgb rowBg = alternateRow ? LIGHT_GRAY : new DeviceRgb(255, 255, 255);
                alternateRow = !alternateRow;

                addCell(table, c.getNumeroCh() != null ? c.getNumeroCh() : "", rowBg);
                addCell(table, c.getDataAbertura() != null ? c.getDataAbertura().format(DTF) : "", rowBg);
                addCell(table, c.getDataFechamento() != null ? c.getDataFechamento().format(DTF) : "-", rowBg);
                addCell(table, c.getEquipamento() != null ? c.getEquipamento().getNome() : "-", rowBg);
                addCell(table, c.getSetor() != null ? c.getSetor().getNome() : "-", rowBg);
                addCell(table, c.getMotivo() != null ? c.getMotivo().getDescricao() : "-", rowBg);
                addCell(table, c.getDescricaoAtendimento() != null ? c.getDescricaoAtendimento() : "", rowBg);
                addCell(table, c.tempoFormatado(), rowBg);
                addCell(table, c.getStatus() != null ? c.getStatus().getDescricao() : "", rowBg);
                addCell(table, String.valueOf(c.getOcorrenciasMesmoServico() != null ? c.getOcorrenciasMesmoServico() : 0), rowBg);
                addCell(table, c.getConceito() != null ? c.getConceito().getDescricao() : "-", rowBg);
                addCell(table, c.getObservacao() != null ? c.getObservacao() : "", rowBg);
            }

            document.add(table);

            // Rodapé do prestador
            document.add(new Paragraph("Total: " + chamados.size() + " chamados no período")
                .setFontSize(8).setBold().setTextAlignment(TextAlignment.RIGHT).setMarginTop(3));
        }

        document.close();
        return out.toByteArray();
    }

    private void addCell(Table table, String value, DeviceRgb bgColor) {
        table.addCell(new Cell()
            .add(new Paragraph(value != null ? value : "").setFontSize(7))
            .setBackgroundColor(bgColor)
            .setPadding(2)
            .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE));
    }
}
