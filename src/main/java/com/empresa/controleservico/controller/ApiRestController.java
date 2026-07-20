package com.empresa.controleservico.controller;

import com.empresa.controleservico.entity.Equipamento;
import com.empresa.controleservico.service.EquipamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * API JSON de apoio à interface, voltada a consultas de dados mestres e seletores dinâmicos.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiRestController {

    private final EquipamentoService equipamentoService;
    private final com.empresa.controleservico.repository.PrestadorRepository  prestadorRepository;
    private final com.empresa.controleservico.repository.SetorRepository      setorRepository;
    private final com.empresa.controleservico.repository.MotivoRepository     motivoRepository;

    // Equipamentos por prestador (AJAX select)
    /**
     * Retorna equipamentos ativos de um prestador e, durante edição, pode incluir
     * o equipamento atual mesmo que esteja inativo.
     *
     * @param prestadorId identificador obrigatório do prestador
     * @param incluirId identificador opcional do equipamento atual
     * @return lista JSON com identificador e nome de exibição
     */
    @GetMapping("/equipamentos")
    public ResponseEntity<List<Map<String, Object>>> equipamentosPorPrestador(
            @RequestParam Long prestadorId,
            @RequestParam(required = false) Long incluirId) {
        List<Equipamento> equips = equipamentoService.listarPorPrestador(prestadorId, incluirId);
        List<Map<String, Object>> result = equips.stream().map(e -> Map.<String, Object>of(
            "id",   e.getId(),
            "nome", e.getNome() + (e.getModelo() != null ? " — " + e.getModelo() : "")
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Prestadores ativos
    /**
     * Retorna os prestadores ativos para consumo pela interface.
     *
     * @return lista JSON de prestadores
     */
    @GetMapping("/prestadores")
    public ResponseEntity<?> prestadores() {
        return ResponseEntity.ok(prestadorRepository.findByAtivoTrueOrderByNomeAsc()
            .stream().map(p -> Map.of("id", p.getId(), "nome", p.getNome()))
            .collect(Collectors.toList()));
    }

    // Setores
    /**
     * Retorna os setores ativos.
     *
     * @return lista JSON de setores
     */
    @GetMapping("/setores")
    public ResponseEntity<?> setores() {
        return ResponseEntity.ok(setorRepository.findByAtivoTrueOrderByNomeAsc()
            .stream().map(s -> Map.of("id", s.getId(), "nome", s.getNome()))
            .collect(Collectors.toList()));
    }

    // Motivos
    /**
     * Retorna todos os motivos ordenados por descrição.
     *
     * @return lista JSON de motivos
     */
    @GetMapping("/motivos")
    public ResponseEntity<?> motivos() {
        return ResponseEntity.ok(motivoRepository.findAllByOrderByDescricaoAsc()
            .stream().map(m -> Map.of("id", m.getId(), "descricao", m.getDescricao()))
            .collect(Collectors.toList()));
    }
}
