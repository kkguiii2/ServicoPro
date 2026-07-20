package com.empresa.controleservico.repository;

import com.empresa.controleservico.entity.ChamadoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistência do histórico de transições de chamados.
 */
public interface ChamadoHistoricoRepository extends JpaRepository<ChamadoHistorico, Long> {
    /**
     * @param chamadoId identificador do chamado
     * @return transições da mais recente para a mais antiga
     */
    List<ChamadoHistorico> findByChamadoIdOrderByAlteradoEmDesc(Long chamadoId);
}
