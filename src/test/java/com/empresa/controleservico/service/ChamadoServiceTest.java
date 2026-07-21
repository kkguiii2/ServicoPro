package com.empresa.controleservico.service;

import com.empresa.controleservico.dto.AlterarStatusForm;
import com.empresa.controleservico.dto.ChamadoForm;
import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.entity.ChamadoHistorico;
import com.empresa.controleservico.entity.Equipamento;
import com.empresa.controleservico.entity.Motivo;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.entity.Setor;
import com.empresa.controleservico.enums.StatusChamado;
import com.empresa.controleservico.repository.ChamadoHistoricoRepository;
import com.empresa.controleservico.repository.ChamadoRepository;
import com.empresa.controleservico.repository.EquipamentoRepository;
import com.empresa.controleservico.repository.MotivoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import com.empresa.controleservico.repository.SetorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock private ChamadoRepository chamadoRepository;
    @Mock private PrestadorRepository prestadorRepository;
    @Mock private EquipamentoRepository equipamentoRepository;
    @Mock private SetorRepository setorRepository;
    @Mock private MotivoRepository motivoRepository;
    @Mock private ChamadoHistoricoRepository historicoRepository;

    private ChamadoService service;

    @BeforeEach
    void setUp() {
        service = new ChamadoService(
            chamadoRepository,
            prestadorRepository,
            equipamentoRepository,
            setorRepository,
            motivoRepository,
            historicoRepository
        );
    }

    @Test
    void reabrirChamadoLimpaDadosDeFechamentoERegistraEstadoPersistido() {
        Chamado chamado = Chamado.builder()
            .id(1L)
            .status(StatusChamado.CONCLUIDO)
            .dataAbertura(LocalDateTime.of(2026, 7, 1, 8, 0))
            .dataFechamento(LocalDateTime.of(2026, 7, 1, 10, 0))
            .tempoAtendimentoMinutos(120)
            .build();
        when(chamadoRepository.findDetalhadoPorId(1L)).thenReturn(Optional.of(chamado));

        service.alterarStatus(1L, new AlterarStatusForm(
            StatusChamado.ABERTO,
            "Atendimento reaberto",
            null
        ));

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getDataFechamento()).isNull();
        assertThat(chamado.getTempoAtendimentoMinutos()).isNull();

        ArgumentCaptor<ChamadoHistorico> captor = ArgumentCaptor.forClass(ChamadoHistorico.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatusPara()).isEqualTo(StatusChamado.ABERTO);
    }

    @Test
    void editarSemEquipamentoRemoveAssociacaoAnterior() {
        Prestador prestador = Prestador.builder().id(1L).nome("Prestador").ativo(true).build();
        Equipamento equipamento = Equipamento.builder().id(2L).nome("Impressora").prestador(prestador).build();
        Setor setor = Setor.builder().id(3L).nome("TI").ativo(true).build();
        Motivo motivo = Motivo.builder().id(4L).descricao("Falha").build();
        Chamado chamado = Chamado.builder()
            .id(5L)
            .prestador(prestador)
            .equipamento(equipamento)
            .setor(setor)
            .motivo(motivo)
            .status(StatusChamado.ABERTO)
            .build();
        ChamadoForm form = ChamadoForm.builder()
            .prestadorId(1L)
            .setorId(3L)
            .motivoId(4L)
            .descricaoAtendimento("Atendimento")
            .dataAbertura(LocalDateTime.of(2026, 7, 1, 8, 0))
            .build();

        when(chamadoRepository.findDetalhadoPorId(5L)).thenReturn(Optional.of(chamado));
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));
        when(setorRepository.findById(3L)).thenReturn(Optional.of(setor));
        when(motivoRepository.findById(4L)).thenReturn(Optional.of(motivo));

        service.editar(5L, form);

        assertThat(chamado.getEquipamento()).isNull();
    }

    @Test
    void rejeitaEquipamentoDeOutroPrestador() {
        Prestador selecionado = Prestador.builder().id(1L).nome("Selecionado").ativo(true).build();
        Prestador proprietario = Prestador.builder().id(2L).nome("Proprietário").ativo(true).build();
        Equipamento equipamento = Equipamento.builder().id(3L).nome("Servidor").prestador(proprietario).build();
        ChamadoForm form = ChamadoForm.builder()
            .prestadorId(1L)
            .equipamentoId(3L)
            .setorId(4L)
            .motivoId(5L)
            .descricaoAtendimento("Atendimento")
            .dataAbertura(LocalDateTime.of(2026, 7, 1, 8, 0))
            .build();

        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(selecionado));
        when(equipamentoRepository.findById(3L)).thenReturn(Optional.of(equipamento));
        assertThatThrownBy(() -> service.criar(form))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("prestador");
    }

    @Test
    void callbackCalculaTempoSemAlterarStatusSolicitado() {
        Chamado chamado = Chamado.builder()
            .status(StatusChamado.ABERTO)
            .dataAbertura(LocalDateTime.of(2026, 7, 1, 8, 0))
            .dataFechamento(LocalDateTime.of(2026, 7, 1, 9, 0))
            .build();

        chamado.calcularTempo();

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getTempoAtendimentoMinutos()).isEqualTo(60);
    }

    @Test
    void listarNormalizaNumeroChAusenteComoTextoVazio() {
        service.listar(
            null, null, null, null, null,
            null, null, null, null,
            0, 20, "dataAbertura", "desc"
        );

        ArgumentCaptor<String> numeroChCaptor = ArgumentCaptor.forClass(String.class);
        verify(chamadoRepository).findWithFilters(
            nullable(Long.class),
            nullable(StatusChamado.class),
            nullable(Long.class),
            nullable(Long.class),
            nullable(Long.class),
            nullable(LocalDateTime.class),
            nullable(LocalDateTime.class),
            nullable(com.empresa.controleservico.enums.ConceitoAvaliacao.class),
            numeroChCaptor.capture(),
            any(org.springframework.data.domain.Pageable.class)
        );
        assertThat(numeroChCaptor.getValue()).isEmpty();
    }
}
