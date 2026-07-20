package com.empresa.controleservico.service;

import com.empresa.controleservico.dto.EquipamentoForm;
import com.empresa.controleservico.entity.Equipamento;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.repository.EquipamentoRepository;
import com.empresa.controleservico.repository.PrestadorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Centraliza consultas e alterações de equipamentos vinculados a prestadores.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final PrestadorRepository   prestadorRepository;

    /**
     * @return todos os equipamentos com o prestador carregado
     */
    public List<Equipamento> listarTodos() {
        return equipamentoRepository.findAll();
    }

    /**
     * @return equipamentos ativos ordenados por nome
     */
    public List<Equipamento> listarAtivos() {
        return equipamentoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    /**
     * Lista equipamentos ativos de um prestador, podendo acrescentar o registro
     * atual de uma edição quando ele estiver inativo.
     *
     * @param prestadorId identificador do prestador
     * @param incluirId equipamento opcional a preservar na lista
     * @return equipamentos ordenados por nome
     */
    public List<Equipamento> listarPorPrestador(Long prestadorId, Long incluirId) {
        List<Equipamento> equipamentos = new ArrayList<>(
            equipamentoRepository.findByPrestadorIdAndAtivoTrueOrderByNomeAsc(prestadorId));
        if (incluirId != null && equipamentos.stream().noneMatch(e -> e.getId().equals(incluirId))) {
            equipamentoRepository.findById(incluirId)
                .filter(e -> e.getPrestador() != null && e.getPrestador().getId().equals(prestadorId))
                .ifPresent(equipamentos::add);
        }
        equipamentos.sort(Comparator.comparing(Equipamento::getNome, String.CASE_INSENSITIVE_ORDER));
        return equipamentos;
    }

    /**
     * @param id identificador do equipamento
     * @return equipamento encontrado
     */
    public Equipamento buscarPorId(Long id) {
        return equipamentoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Equipamento #" + id + " não encontrado"));
    }

    /**
     * Cria um equipamento ativo associado a um prestador existente.
     *
     * @param form dados de cadastro
     * @return equipamento persistido
     */
    @Transactional
    public Equipamento salvar(EquipamentoForm form) {
        Prestador prestador = prestadorRepository.findById(form.getPrestadorId())
            .orElseThrow(() -> new EntityNotFoundException("Prestador não encontrado"));
        Equipamento equipamento = Equipamento.builder()
            .nome(form.getNome().trim())
            .modelo(form.getModelo())
            .numeroSerie(form.getNumeroSerie())
            .prestador(prestador)
            .ativo(true)
            .build();
        return equipamentoRepository.save(equipamento);
    }

    /**
     * Exclui fisicamente um equipamento, se a integridade referencial permitir.
     *
     * @param id identificador do equipamento
     */
    @Transactional
    public void excluir(Long id) {
        equipamentoRepository.deleteById(id);
    }
}
