package com.empresa.controleservico.repository;

import com.empresa.controleservico.entity.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistência e consultas derivadas de prestadores.
 */
public interface PrestadorRepository extends JpaRepository<Prestador, Long> {
    /** @return prestadores ativos ordenados por nome */
    List<Prestador> findByAtivoTrueOrderByNomeAsc();
}
