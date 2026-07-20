package com.empresa.controleservico.entity;

import com.empresa.controleservico.enums.ConceitoAvaliacao;
import com.empresa.controleservico.enums.StatusChamado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Registro central de atendimento, associado a prestador, setor, motivo e equipamento opcional.
 * Mantém estado, avaliação, dados temporais e contador de reincidência.
 */
@Entity
@Table(name = "chamados", indexes = {
    @Index(name = "idx_chamados_status",    columnList = "status"),
    @Index(name = "idx_chamados_prestador", columnList = "prestador_id"),
    @Index(name = "idx_chamados_abertura",  columnList = "data_abertura")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Chamado {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ch", length = 50)
    private String numeroCh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_id", nullable = false)
    private Motivo motivo;

    @Column(name = "descricao_atendimento", nullable = false, columnDefinition = "TEXT")
    private String descricaoAtendimento;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    // Tempo em minutos (calculado automaticamente)
    @Column(name = "tempo_atendimento_minutos")
    private Integer tempoAtendimentoMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusChamado status = StatusChamado.ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "conceito")
    private ConceitoAvaliacao conceito;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "ocorrencias_mesmo_servico")
    @Builder.Default
    private Integer ocorrenciasMesmoServico = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Lifecycle Callbacks ──────────────────────────────────────────────────

    /**
     * Recalcula o tempo persistido quando abertura e fechamento formam um intervalo válido.
     */
    @PrePersist
    @PreUpdate
    public void calcularTempo() {
        if (dataAbertura != null && dataFechamento != null
                && !dataFechamento.isBefore(dataAbertura)) {
            tempoAtendimentoMinutos = (int) Duration.between(dataAbertura, dataFechamento).toMinutes();
        } else {
            tempoAtendimentoMinutos = null;
        }
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    /**
     * Formata o tempo de atendimento em horas e minutos.
     * Ex: "2h 30min", "45min", "—"
     *
     * @return duração formatada ou indicação de chamado ainda aberto
     */
    public String tempoFormatado() {
        if (tempoAtendimentoMinutos == null || tempoAtendimentoMinutos <= 0) {
            // Calcula em tempo real se ainda aberto
            if (dataAbertura != null && dataFechamento == null
                    && status != StatusChamado.CANCELADO) {
                long min = Duration.between(dataAbertura, LocalDateTime.now()).toMinutes();
                return formatarMinutos((int) min) + " (em aberto)";
            }
            return "—";
        }
        return formatarMinutos(tempoAtendimentoMinutos);
    }

    private String formatarMinutos(int minutos) {
        if (minutos < 60) return minutos + "min";
        int h = minutos / 60;
        int m = minutos % 60;
        return m > 0 ? h + "h " + m + "min" : h + "h";
    }

    /**
     * Indica se o chamado é reincidente (2+ ocorrências do mesmo serviço).
     *
     * @return {@code true} quando o contador persistido é igual ou superior a dois
     */
    public boolean isReincidente() {
        return ocorrenciasMesmoServico != null && ocorrenciasMesmoServico >= 2;
    }

    /**
     * Indica se o chamado está concluído ou cancelado.
     *
     * @return {@code true} para estados terminais
     */
    public boolean isFinalizado() {
        return status == StatusChamado.CONCLUIDO || status == StatusChamado.CANCELADO;
    }
}
