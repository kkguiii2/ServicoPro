package com.empresa.controleservico.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Classificação única do motivo que originou um chamado.
 */
@Entity
@Table(name = "motivos")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Motivo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String descricao;
}
