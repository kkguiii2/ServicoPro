package com.empresa.controleservico.service;

import com.empresa.controleservico.dto.PrestadorForm;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.repository.PrestadorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Centraliza consultas e alterações do cadastro de prestadores.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrestadorService {

    private final PrestadorRepository prestadorRepository;

    /**
     * @return todos os prestadores cadastrados
     */
    public List<Prestador> listarTodos() {
        return prestadorRepository.findAll();
    }

    /**
     * @return prestadores ativos ordenados por nome
     */
    public List<Prestador> listarAtivos() {
        return prestadorRepository.findByAtivoTrueOrderByNomeAsc();
    }

    /**
     * Busca um prestador ou falha quando o identificador não existe.
     *
     * @param id identificador do prestador
     * @return prestador encontrado
     */
    public Prestador buscarPorId(Long id) {
        return prestadorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Prestador #" + id + " não encontrado"));
    }

    /**
     * Cria um prestador ativo a partir dos dados validados.
     *
     * @param form dados de cadastro
     * @return prestador persistido
     */
    @Transactional
    public Prestador salvar(PrestadorForm form) {
        Prestador prestador = Prestador.builder()
            .nome(form.getNome().trim())
            .descricao(form.getDescricao())
            .ativo(true)
            .build();
        return prestadorRepository.save(prestador);
    }

    /**
     * Desativa logicamente um prestador sem remover seu histórico.
     *
     * @param id identificador do prestador
     * @return estado persistido
     */
    @Transactional
    public Prestador desativar(Long id) {
        Prestador p = buscarPorId(id);
        p.setAtivo(false);
        return prestadorRepository.save(p);
    }

    /**
     * Reativa um prestador.
     *
     * @param id identificador do prestador
     * @return estado persistido
     */
    @Transactional
    public Prestador ativar(Long id) {
        Prestador p = buscarPorId(id);
        p.setAtivo(true);
        return prestadorRepository.save(p);
    }

    /**
     * Exclui fisicamente um prestador, se a integridade referencial permitir.
     *
     * @param id identificador do prestador
     */
    @Transactional
    public void excluir(Long id) {
        prestadorRepository.deleteById(id);
    }
}
