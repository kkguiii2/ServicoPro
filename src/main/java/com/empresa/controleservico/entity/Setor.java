package com.empresa.controleservico.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Setor ou solicitante interno associado ao chamado.
 */
@Entity
@Table(name = "setores")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Setor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
