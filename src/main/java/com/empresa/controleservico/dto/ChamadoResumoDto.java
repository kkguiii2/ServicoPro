package com.empresa.controleservico.dto;

import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.enums.ConceitoAvaliacao;
import com.empresa.controleservico.enums.StatusChamado;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Projeção resumida de chamado usada no dashboard e em listas recentes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChamadoResumoDto {

    private Long id;
    private String numeroCh;
    private String prestadorNome;
    private String setorNome;
    private String motivoDescricao;
    private LocalDateTime dataAbertura;
    private StatusChamado status;
    private ConceitoAvaliacao conceito;
    private String tempoFormatado;
    private boolean reincidente;

    /**
     * Cria a projeção a partir de uma entidade com associações já carregadas.
     *
     * @param c chamado de origem
     * @return resumo pronto para apresentação
     */
    public static ChamadoResumoDto from(Chamado c) {
        return ChamadoResumoDto.builder()
            .id(c.getId())
            .numeroCh(c.getNumeroCh())
            .prestadorNome(c.getPrestador() != null ? c.getPrestador().getNome() : null)
            .setorNome(c.getSetor() != null ? c.getSetor().getNome() : null)
            .motivoDescricao(c.getMotivo() != null ? c.getMotivo().getDescricao() : null)
            .dataAbertura(c.getDataAbertura())
            .status(c.getStatus())
            .conceito(c.getConceito())
            .tempoFormatado(c.tempoFormatado())
            .reincidente(c.isReincidente())
            .build();
    }
}
