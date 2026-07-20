package com.empresa.controleservico.repository;

import com.empresa.controleservico.entity.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistência e consultas derivadas de setores.
 */
public interface SetorRepository extends JpaRepository<Setor, Long> {
    /** @return setores ativos ordenados por nome */
    List<Setor> findByAtivoTrueOrderByNomeAsc();
    /** @return todos os setores ordenados por nome */
    List<Setor> findAllByOrderByNomeAsc();
}
