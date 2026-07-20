package com.empresa.controleservico.entity;

import com.empresa.controleservico.enums.StatusChamado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Auditoria de cada transição de status de um chamado, incluindo observação e instante da mudança.
 */
@Entity
@Table(name = "chamado_historico")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChamadoHistorico {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_de")
    private StatusChamado statusDe;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_para", nullable = false)
    private StatusChamado statusPara;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @CreationTimestamp
    @Column(name = "alterado_em", updatable = false)
    private LocalDateTime alteradoEm;
}
