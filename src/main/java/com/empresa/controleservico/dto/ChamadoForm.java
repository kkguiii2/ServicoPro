package com.empresa.controleservico.dto;

import com.empresa.controleservico.enums.ConceitoAvaliacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Dados validados para abertura e edição de chamados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamadoForm {

    @Size(max = 50, message = "Número CH deve ter no máximo 50 caracteres")
    private String numeroCh;

    @NotNull(message = "Prestador é obrigatório")
    private Long prestadorId;

    private Long equipamentoId;

    @NotNull(message = "Setor é obrigatório")
    private Long setorId;

    @NotNull(message = "Motivo é obrigatório")
    private Long motivoId;

    @NotBlank(message = "Descrição do atendimento é obrigatória")
    @Size(max = 10000, message = "Descrição deve ter no máximo 10000 caracteres")
    private String descricaoAtendimento;

    @NotNull(message = "Data de abertura é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataAbertura;

    private ConceitoAvaliacao conceito;

    @Size(max = 5000, message = "Observação deve ter no máximo 5000 caracteres")
    private String observacao;
}
