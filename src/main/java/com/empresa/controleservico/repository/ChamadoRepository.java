package com.empresa.controleservico.repository;

import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.enums.ConceitoAvaliacao;
import com.empresa.controleservico.enums.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Persistência e consultas analíticas de chamados.
 * As consultas de apresentação usam fetch joins para funcionar com Open Session in View desativado.
 */
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    /**
     * @param id identificador do chamado
     * @return chamado com prestador, equipamento, setor e motivo carregados
     */
    @Query("""
        SELECT c FROM Chamado c
        LEFT JOIN FETCH c.prestador
        LEFT JOIN FETCH c.equipamento
        LEFT JOIN FETCH c.setor
        LEFT JOIN FETCH c.motivo
        WHERE c.id = :id
    """)
    Optional<Chamado> findDetalhadoPorId(@Param("id") Long id);

    // ── Paginação com filtros dinâmicos ──────────────────────────────────────

    /**
     * Aplica os filtros opcionais e a paginação recebida.
     *
     * @param prestadorId prestador opcional
     * @param status estado opcional
     * @param setorId setor opcional
     * @param motivoId motivo opcional
     * @param equipamentoId equipamento opcional
     * @param dataInicio limite inferior inclusivo da abertura
     * @param dataFim limite superior exclusivo da abertura
     * @param conceito avaliação opcional
     * @param numeroCh trecho opcional do número externo
     * @param pageable paginação e ordenação validadas pelo service
     * @return página de chamados detalhados
     */
    @Query("""
        SELECT c FROM Chamado c
        LEFT JOIN FETCH c.prestador
        LEFT JOIN FETCH c.equipamento
        LEFT JOIN FETCH c.setor
        LEFT JOIN FETCH c.motivo
        WHERE (:prestadorId IS NULL OR c.prestador.id = :prestadorId)
          AND (:status      IS NULL OR c.status       = :status)
          AND (:setorId     IS NULL OR c.setor.id     = :setorId)
          AND (:motivoId    IS NULL OR c.motivo.id    = :motivoId)
          AND (:equipamentoId IS NULL OR c.equipamento.id = :equipamentoId)
          AND (:dataInicio  IS NULL OR c.dataAbertura >= :dataInicio)
          AND (:dataFim     IS NULL OR c.dataAbertura < :dataFim)
          AND (:conceito    IS NULL OR c.conceito     = :conceito)
          AND (:numeroCh    IS NULL OR LOWER(c.numeroCh) LIKE LOWER(CONCAT('%', :numeroCh, '%')))
    """)
    Page<Chamado> findWithFilters(
        @Param("prestadorId")    Long prestadorId,
        @Param("status")         StatusChamado status,
        @Param("setorId")        Long setorId,
        @Param("motivoId")       Long motivoId,
        @Param("equipamentoId")  Long equipamentoId,
        @Param("dataInicio")     LocalDateTime dataInicio,
        @Param("dataFim")        LocalDateTime dataFim,
        @Param("conceito")       ConceitoAvaliacao conceito,
        @Param("numeroCh")       String numeroCh,
        Pageable pageable
    );

    // ── Relatórios ───────────────────────────────────────────────────────────

    /**
     * @param prestadorId prestador incluído no relatório
     * @param inicio limite inicial inclusivo
     * @param fimExclusive limite final exclusivo
     * @return chamados de um prestador em um intervalo semiaberto, ordenados por abertura
     */
    @Query("""
        SELECT c FROM Chamado c
        LEFT JOIN FETCH c.equipamento
        LEFT JOIN FETCH c.setor
        LEFT JOIN FETCH c.motivo
        WHERE c.prestador.id = :prestadorId
          AND c.dataAbertura >= :inicio
          AND c.dataAbertura < :fimExclusive
        ORDER BY c.dataAbertura ASC
    """)
    List<Chamado> findParaRelatorio(
        @Param("prestadorId") Long prestadorId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fimExclusive") LocalDateTime fimExclusive
    );

    // ── Dashboard KPIs ───────────────────────────────────────────────────────

    /**
     * @param prestadorId prestador opcional
     * @param inicio limite inicial inclusivo
     * @param fimExclusive limite final exclusivo
     * @return quantidade de chamados no intervalo semiaberto
     */
    @Query("""
        SELECT COUNT(c) FROM Chamado c
        WHERE (:prestadorId IS NULL OR c.prestador.id = :prestadorId)
          AND c.dataAbertura >= :inicio
          AND c.dataAbertura < :fimExclusive
    """)
    long countNoPeriodo(
        @Param("prestadorId") Long prestadorId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fimExclusive") LocalDateTime fimExclusive
    );

    /**
     * @param prestadorId prestador opcional
     * @return média global ou por prestador do tempo persistido, em minutos
     */
    @Query("SELECT AVG(c.tempoAtendimentoMinutos) FROM Chamado c WHERE c.tempoAtendimentoMinutos IS NOT NULL AND (:prestadorId IS NULL OR c.prestador.id = :prestadorId)")
    Double avgTempoAtendimento(@Param("prestadorId") Long prestadorId);

    /**
     * @param prestadorId prestador opcional
     * @param inicio limite inicial inclusivo
     * @param fimExclusive limite final exclusivo
     * @return linhas {@code [prestadorId, nome, quantidade]} no intervalo semiaberto
     */
    @Query("""
        SELECT c.prestador.id, c.prestador.nome, COUNT(c) FROM Chamado c
        WHERE (:prestadorId IS NULL OR c.prestador.id = :prestadorId)
          AND c.dataAbertura >= :inicio
          AND c.dataAbertura < :fimExclusive
        GROUP BY c.prestador.id, c.prestador.nome
        ORDER BY c.prestador.nome
    """)
    List<Object[]> countPorPrestadorPeriodo(
        @Param("prestadorId") Long prestadorId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fimExclusive") LocalDateTime fimExclusive
    );

    /**
     * @param prestadorId prestador opcional
     * @return linhas {@code [status, quantidade]} com contagens históricas
     */
    @Query("SELECT c.status, COUNT(c) FROM Chamado c WHERE (:prestadorId IS NULL OR c.prestador.id = :prestadorId) GROUP BY c.status")
    List<Object[]> countPorStatus(@Param("prestadorId") Long prestadorId);

    /**
     * @param prestadorId prestador opcional
     * @param pageable limite e ordenação do ranking
     * @return linhas {@code [descricao, quantidade]} ordenadas por frequência
     */
    @Query("SELECT c.motivo.descricao, COUNT(c) FROM Chamado c WHERE (:prestadorId IS NULL OR c.prestador.id = :prestadorId) GROUP BY c.motivo.descricao ORDER BY COUNT(c) DESC")
    List<Object[]> countPorMotivo(@Param("prestadorId") Long prestadorId, Pageable pageable);

    // ── Reincidência ─────────────────────────────────────────────────────────

    /**
     * @param prestadorId prestador opcional
     * @param inicio limite inicial inclusivo
     * @param fimExclusive limite final exclusivo
     * @param minOcorrencias limiar mínimo do contador persistido
     * @return quantidade de chamados marcados como reincidentes no intervalo
     */
    @Query("""
        SELECT COUNT(c) FROM Chamado c
        WHERE (:prestadorId IS NULL OR c.prestador.id = :prestadorId)
          AND c.dataAbertura >= :inicio
          AND c.dataAbertura < :fimExclusive
          AND c.ocorrenciasMesmoServico >= :minOcorrencias
    """)
    long countReincidentes(
        @Param("prestadorId")    Long prestadorId,
        @Param("inicio")         LocalDateTime inicio,
        @Param("fimExclusive")   LocalDateTime fimExclusive,
        @Param("minOcorrencias") int minOcorrencias
    );

    /**
     * Conta ocorrências anteriores do mesmo equipamento, motivo e prestador desde a data de corte.
     *
     * @param equipamentoId equipamento que identifica o serviço
     * @param motivoId motivo que identifica o serviço
     * @param prestadorId prestador que identifica o serviço
     * @param desde data de abertura mínima inclusiva
     * @param excluirId chamado desconsiderado no cálculo
     * @return quantidade de ocorrências sem contar o chamado excluído
     */
    @Query("""
        SELECT COUNT(c) FROM Chamado c
        WHERE c.equipamento.id   = :equipamentoId
          AND c.motivo.id        = :motivoId
          AND c.prestador.id     = :prestadorId
          AND c.dataAbertura    >= :desde
          AND c.id              != :excluirId
    """)
    long contarOcorrenciasServico(
        @Param("equipamentoId") Long equipamentoId,
        @Param("motivoId")      Long motivoId,
        @Param("prestadorId")   Long prestadorId,
        @Param("desde")         LocalDateTime desde,
        @Param("excluirId")     Long excluirId
    );

    // ── Últimos chamados ─────────────────────────────────────────────────────

    /**
     * @param prestadorId prestador opcional
     * @param pageable limite da lista
     * @return chamados mais recentes, limitados pelo {@link Pageable}
     */
    @Query("""
        SELECT c FROM Chamado c
        LEFT JOIN FETCH c.prestador
        LEFT JOIN FETCH c.setor
        LEFT JOIN FETCH c.motivo
        WHERE (:prestadorId IS NULL OR c.prestador.id = :prestadorId)
        ORDER BY c.createdAt DESC
    """)
    List<Chamado> findRecentes(@Param("prestadorId") Long prestadorId, Pageable pageable);
}
