package com.empresa.controleservico.service;

import com.empresa.controleservico.dto.ChamadoResumoDto;
import com.empresa.controleservico.dto.DashboardKpiDto;
import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.enums.StatusChamado;
import com.empresa.controleservico.repository.ChamadoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcula os indicadores e séries apresentados no dashboard.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ChamadoRepository    chamadoRepository;
    private final PrestadorRepository  prestadorRepository;

    /**
     * Consolida contagens, reincidência, tempo médio, séries mensais, motivos e registros recentes.
     * O filtro por prestador é aplicado a todas as consultas.
     *
     * @param prestadorId identificador opcional do prestador
     * @return DTO completo do dashboard
     */
    public DashboardKpiDto calcularKpis(Long prestadorId) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioMes = agora.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicio12m = inicioMes.minusMonths(11);
        LocalDateTime fimMesExclusive = inicioMes.plusMonths(1);

        Prestador prestadorSelecionado = prestadorId == null
            ? null
            : prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Prestador não encontrado"));

        List<Object[]> statusData = chamadoRepository.countPorStatus(prestadorId);
        Map<String, Long> chamadosPorStatus = criarMapaStatus(statusData);
        long abertos = chamadosPorStatus.get(StatusChamado.ABERTO.name());
        long emAndamento = chamadosPorStatus.get(StatusChamado.EM_ANDAMENTO.name());
        long concluidos = chamadosPorStatus.get(StatusChamado.CONCLUIDO.name());
        long cancelados = chamadosPorStatus.get(StatusChamado.CANCELADO.name());
        long totalMes = chamadoRepository.countNoPeriodo(prestadorId, inicioMes, fimMesExclusive);

        long reincidentes = chamadoRepository.countReincidentes(prestadorId, inicio12m, agora, 2);
        long total12m = chamadoRepository.countNoPeriodo(prestadorId, inicio12m, agora);
        double taxaReincidencia = total12m > 0
            ? Math.round((reincidentes * 100.0 / total12m) * 10.0) / 10.0
            : 0.0;

        // Tempo médio (horas)
        Double tempoMedioMin = chamadoRepository.avgTempoAtendimento(prestadorId);
        double tempoMedioHoras = tempoMedioMin != null
            ? Math.round((tempoMedioMin / 60.0) * 10.0) / 10.0
            : 0.0;

        // Chamados por prestador
        List<Prestador> prestadores = prestadorSelecionado == null
            ? prestadorRepository.findByAtivoTrueOrderByNomeAsc()
            : List.of(prestadorSelecionado);
        List<String> prestadoresNomes    = new ArrayList<>();
        List<Long>   chamadosPorPrestador = new ArrayList<>();

        List<Object[]> dadosPorPrestador = chamadoRepository
            .countPorPrestadorPeriodo(prestadorId, inicio12m, agora);
        Map<Long, Long> mapPrestador = dadosPorPrestador.stream()
            .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[2]));

        for (Prestador p : prestadores) {
            prestadoresNomes.add(p.getNome());
            chamadosPorPrestador.add(mapPrestador.getOrDefault(p.getId(), 0L));
        }

        // Evolução por mês (últimos 6 meses)
        List<String> mesesLabels   = new ArrayList<>();
        List<Long>   chamadosPorMes = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime m  = agora.minusMonths(i);
            LocalDateTime ini = m.toLocalDate().withDayOfMonth(1).atStartOfDay();
            LocalDateTime fimExclusive = ini.plusMonths(1);
            mesesLabels.add(m.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR")));
            chamadosPorMes.add(chamadoRepository.countNoPeriodo(prestadorId, ini, fimExclusive));
        }

        // Top 5 motivos
        List<Object[]> motivosData = chamadoRepository.countPorMotivo(prestadorId, PageRequest.of(0, 5));
        List<String> top5MotivosLabels = new ArrayList<>();
        List<Long>   top5MotivosCount  = new ArrayList<>();
        for (Object[] row : motivosData) {
            top5MotivosLabels.add((String) row[0]);
            top5MotivosCount.add((Long) row[1]);
        }

        // Últimos 10 chamados
        List<ChamadoResumoDto> ultimos = chamadoRepository
            .findRecentes(prestadorId, PageRequest.of(0, 10))
            .stream()
            .map(ChamadoResumoDto::from)
            .collect(Collectors.toList());

        return DashboardKpiDto.builder()
            .totalChamadosMes(totalMes)
            .chamadosAbertos(abertos)
            .chamadosConcluidos(concluidos)
            .chamadosEmAndamento(emAndamento)
            .chamadosCancelados(cancelados)
            .taxaReincidencia(taxaReincidencia)
            .tempoMedioAtendimentoHoras(tempoMedioHoras)
            .prestadoresNomes(prestadoresNomes)
            .chamadosPorPrestador(chamadosPorPrestador)
            .chamadosPorStatus(chamadosPorStatus)
            .mesesLabels(mesesLabels)
            .chamadosPorMes(chamadosPorMes)
            .top5MotivosLabels(top5MotivosLabels)
            .top5MotivosCount(top5MotivosCount)
            .ultimosChamados(ultimos)
            .build();
    }

    private Map<String, Long> criarMapaStatus(List<Object[]> statusData) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (StatusChamado status : StatusChamado.values()) {
            resultado.put(status.name(), 0L);
        }
        for (Object[] row : statusData) {
            resultado.put(row[0].toString(), (Long) row[1]);
        }
        return resultado;
    }
}
