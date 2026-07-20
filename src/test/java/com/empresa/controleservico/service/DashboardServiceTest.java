package com.empresa.controleservico.service;

import com.empresa.controleservico.dto.DashboardKpiDto;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.enums.StatusChamado;
import com.empresa.controleservico.repository.ChamadoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Test
    void aplicaPrestadorEmTodasAsMetricas() {
        ChamadoRepository chamadoRepository = mock(ChamadoRepository.class);
        PrestadorRepository prestadorRepository = mock(PrestadorRepository.class);
        DashboardService service = new DashboardService(chamadoRepository, prestadorRepository);
        Prestador prestador = Prestador.builder().id(7L).nome("Prestador 7").ativo(true).build();

        when(prestadorRepository.findById(7L)).thenReturn(Optional.of(prestador));
        when(chamadoRepository.countPorStatus(7L)).thenReturn(List.<Object[]>of(
            new Object[]{StatusChamado.ABERTO, 2L},
            new Object[]{StatusChamado.CONCLUIDO, 3L}
        ));
        when(chamadoRepository.countNoPeriodo(eq(7L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(5L);
        when(chamadoRepository.countReincidentes(eq(7L), any(LocalDateTime.class), any(LocalDateTime.class), eq(2)))
            .thenReturn(1L);
        when(chamadoRepository.avgTempoAtendimento(7L)).thenReturn(60.0);
        when(chamadoRepository.countPorPrestadorPeriodo(eq(7L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.<Object[]>of(new Object[]{7L, "Prestador 7", 5L}));
        when(chamadoRepository.countPorMotivo(eq(7L), any(Pageable.class))).thenReturn(List.of());
        when(chamadoRepository.findRecentes(eq(7L), any(Pageable.class))).thenReturn(List.of());

        DashboardKpiDto resultado = service.calcularKpis(7L);

        assertThat(resultado.getChamadosAbertos()).isEqualTo(2);
        assertThat(resultado.getChamadosConcluidos()).isEqualTo(3);
        assertThat(resultado.getPrestadoresNomes()).containsExactly("Prestador 7");
        verify(chamadoRepository).findRecentes(eq(7L), any(Pageable.class));
        verify(chamadoRepository).countReincidentes(eq(7L), any(), any(), eq(2));
    }
}
