package com.empresa.controleservico.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Equipamento atendido, pertencente obrigatoriamente a um prestador.
 */
@Entity
@Table(name = "equipamentos", uniqueConstraints = {
    @UniqueConstraint(name = "uk_equipamento_nome_prestador", columnNames = {"nome", "prestador_id"})
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Equipamento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String modelo;

    @Column(name = "numero_serie")
    private String numeroSerie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
