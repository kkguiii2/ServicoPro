package com.empresa.controleservico.dto;

import com.empresa.controleservico.enums.StatusChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Dados necessários para alterar o status e registrar sua justificativa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlterarStatusForm {

    @NotNull(message = "Status é obrigatório")
    private StatusChamado novoStatus;

    @NotBlank(message = "Observação é obrigatória ao alterar status")
    private String observacao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataFechamento;
}
