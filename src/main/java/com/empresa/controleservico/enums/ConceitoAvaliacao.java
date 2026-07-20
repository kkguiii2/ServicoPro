package com.empresa.controleservico.enums;

/**
 * Escala opcional de avaliação do atendimento, da melhor à pior classificação.
 */
public enum ConceitoAvaliacao {
    /** Avaliação máxima, representada por cinco estrelas. */
    EXCELENTE("Excelente", "★★★★★"),
    /** Avaliação positiva de quatro estrelas. */
    MUITO_BOM("Muito Bom", "★★★★☆"),
    /** Avaliação intermediária de três estrelas. */
    BOM("Bom",             "★★★☆☆"),
    /** Avaliação abaixo do esperado, com duas estrelas. */
    REGULAR("Regular",     "★★☆☆☆"),
    /** Avaliação mínima, representada por uma estrela. */
    INSATISFATORIO("Insatisfatório", "★☆☆☆☆");

    private final String descricao;
    private final String estrelas;

    ConceitoAvaliacao(String descricao, String estrelas) {
        this.descricao = descricao;
        this.estrelas  = estrelas;
    }

    /** @return descrição exibida ao usuário */
    public String getDescricao() { return descricao; }
    /** @return representação visual de uma a cinco estrelas */
    public String getEstrelas()  { return estrelas; }
}
