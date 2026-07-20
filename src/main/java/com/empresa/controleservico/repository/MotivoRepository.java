package com.empresa.controleservico.repository;

import com.empresa.controleservico.entity.Motivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistência e consultas derivadas de motivos de chamado.
 */
public interface MotivoRepository extends JpaRepository<Motivo, Long> {
    /** @return todos os motivos ordenados por descrição */
    List<Motivo> findAllByOrderByDescricaoAsc();
}
