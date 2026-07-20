package com.empresa.controleservico.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * Agrega todos os indicadores, séries e registros recentes exibidos no dashboard.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardKpiDto {

    // KPI cards
    private long    totalChamadosMes;
    private long    chamadosAbertos;
    private long    chamadosConcluidos;
    private long    chamadosEmAndamento;
    private long    chamadosCancelados;
    private double  taxaReincidencia;
    private double  tempoMedioAtendimentoHoras;

    // Charts
    private List<String> prestadoresNomes;
    private List<Long>   chamadosPorPrestador;

    private Map<String, Long> chamadosPorStatus;

    private List<String> mesesLabels;
    private List<Long>   chamadosPorMes;

    private List<String> top5MotivosLabels;
    private List<Long>   top5MotivosCount;

    // Tabela recente
    private List<ChamadoResumoDto> ultimosChamados;
}
