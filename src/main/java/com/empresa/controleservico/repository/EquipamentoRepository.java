package com.empresa.controleservico.repository;

import com.empresa.controleservico.entity.Equipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

/**
 * Persistência de equipamentos com carregamento explícito do prestador para uso fora da sessão JPA.
 */
public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {
    /** @return todos os equipamentos com o prestador carregado */
    @Override
    @EntityGraph(attributePaths = "prestador")
    List<Equipamento> findAll();

    /** @return equipamentos ativos ordenados por nome, com o prestador carregado */
    @EntityGraph(attributePaths = "prestador")
    List<Equipamento> findByAtivoTrueOrderByNomeAsc();

    /**
     * @param prestadorId identificador do prestador
     * @return equipamentos ativos desse prestador ordenados por nome
     */
    @EntityGraph(attributePaths = "prestador")
    List<Equipamento> findByPrestadorIdAndAtivoTrueOrderByNomeAsc(Long prestadorId);
}
