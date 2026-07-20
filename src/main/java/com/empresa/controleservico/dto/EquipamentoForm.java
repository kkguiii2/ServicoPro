package com.empresa.controleservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dados validados para cadastro de equipamento.
 */
@Data
@NoArgsConstructor
public class EquipamentoForm {

    @NotNull(message = "Prestador é obrigatório")
    private Long prestadorId;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String nome;

    @Size(max = 255, message = "Modelo deve ter no máximo 255 caracteres")
    private String modelo;

    @Size(max = 255, message = "Número de série deve ter no máximo 255 caracteres")
    private String numeroSerie;
}
