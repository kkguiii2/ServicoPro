package com.empresa.controleservico.enums;

/**
 * Estados persistidos do ciclo de vida de um chamado e metadados de apresentação.
 */
public enum StatusChamado {
    /** Chamado registrado e ainda não iniciado. */
    ABERTO("Aberto", "aberto", "#58A6FF"),
    /** Atendimento em execução. */
    EM_ANDAMENTO("Em Andamento", "andamento", "#D29922"),
    /** Atendimento encerrado com data de fechamento. */
    CONCLUIDO("Concluído", "concluido", "#3FB950"),
    /** Atendimento encerrado sem contabilização de fechamento. */
    CANCELADO("Cancelado", "cancelado", "#F85149");

    private final String descricao;
    private final String cssSuffix;
    private final String hexColor;

    StatusChamado(String descricao, String cssSuffix, String hexColor) {
        this.descricao  = descricao;
        this.cssSuffix  = cssSuffix;
        this.hexColor   = hexColor;
    }

    /** @return descrição exibida ao usuário */
    public String getDescricao()  { return descricao; }
    /** @return sufixo usado nas classes CSS de status */
    public String getCssSuffix()  { return cssSuffix; }
    /** @return cor hexadecimal associada ao status */
    public String getHexColor()   { return hexColor; }
}
