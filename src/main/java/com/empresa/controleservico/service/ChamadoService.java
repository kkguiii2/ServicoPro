package com.empresa.controleservico.service;

import com.empresa.controleservico.dto.AlterarStatusForm;
import com.empresa.controleservico.dto.ChamadoForm;
import com.empresa.controleservico.entity.*;
import com.empresa.controleservico.enums.StatusChamado;
import com.empresa.controleservico.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Implementa as regras transacionais do ciclo de vida de chamados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChamadoService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of(
        "id", "numeroCh", "dataAbertura", "dataFechamento", "status", "createdAt"
    );

    private final ChamadoRepository           chamadoRepository;
    private final PrestadorRepository          prestadorRepository;
    private final EquipamentoRepository        equipamentoRepository;
    private final SetorRepository              setorRepository;
    private final MotivoRepository             motivoRepository;
    private final ChamadoHistoricoRepository   historicoRepository;

    // ── Listar com paginação e filtros ───────────────────────────────────────

    /**
     * Consulta chamados com filtros opcionais, paginação limitada e ordenação permitida.
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
     * @param page índice de página iniciado em zero
     * @param size quantidade de registros, entre 1 e 100
     * @param sortBy propriedade incluída na lista segura de ordenação
     * @param sortDir crescente somente quando igual a {@code asc}; decrescente nos demais casos
     * @return página de chamados com associações necessárias à apresentação
     * @throws IllegalArgumentException quando paginação ou ordenação são inválidas
     */
    public Page<Chamado> listar(
            Long prestadorId,
            StatusChamado status,
            Long setorId,
            Long motivoId,
            Long equipamentoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            com.empresa.controleservico.enums.ConceitoAvaliacao conceito,
            String numeroCh,
            int page, int size,
            String sortBy, String sortDir) {

        if (page < 0) {
            throw new IllegalArgumentException("A página não pode ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("O tamanho da página deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Campo de ordenação inválido");
        }

        Sort sort = Sort.by(
            "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        return chamadoRepository.findWithFilters(
            prestadorId, status, setorId, motivoId, equipamentoId,
            dataInicio, dataFim, conceito,
            numeroCh != null && !numeroCh.isBlank() ? numeroCh : null,
            pageable
        );
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * @param id identificador do chamado
     * @return chamado com suas associações carregadas
     */
    public Chamado buscarPorId(Long id) {
        return chamadoRepository.findDetalhadoPorId(id)
            .orElseThrow(() -> new EntityNotFoundException("Chamado #" + id + " não encontrado"));
    }

    /**
     * Cria um chamado aberto, calcula sua reincidência e registra o histórico inicial.
     *
     * @param form dados validados do chamado
     * @return chamado persistido
     */
    @Transactional
    public Chamado criar(ChamadoForm form) {
        Chamado c = montarEntidade(new Chamado(), form);
        c.setStatus(StatusChamado.ABERTO);
        c.setDataFechamento(null);
        c.setTempoAtendimentoMinutos(null);

        atualizarOcorrencias(c);

        Chamado salvo = chamadoRepository.save(c);

        // Registrar histórico inicial
        registrarHistorico(salvo, null, salvo.getStatus(), "Chamado criado.");
        log.info("Chamado #{} criado - Prestador: {}", salvo.getId(), salvo.getPrestador().getNome());

        return salvo;
    }

    /**
     * Atualiza os dados do chamado e recalcula sua reincidência.
     *
     * @param id identificador do chamado
     * @param form dados validados
     * @return chamado persistido
     */
    @Transactional
    public Chamado editar(Long id, ChamadoForm form) {
        Chamado c = buscarPorId(id);
        montarEntidade(c, form);
        if (c.getStatus() == StatusChamado.CONCLUIDO
                && c.getDataFechamento() != null
                && c.getDataFechamento().isBefore(c.getDataAbertura())) {
            throw new IllegalArgumentException("A data de abertura não pode ser posterior ao fechamento");
        }
        atualizarOcorrencias(c);
        return chamadoRepository.save(c);
    }

    /**
     * Altera o status, controla os dados de fechamento e registra a transição.
     * Reabertura ou cancelamento remove data e tempo de fechamento.
     *
     * @param id identificador do chamado
     * @param form novo estado e justificativa
     */
    @Transactional
    public void alterarStatus(Long id, AlterarStatusForm form) {
        Chamado c = buscarPorId(id);
        StatusChamado statusAnterior = c.getStatus();

        if (statusAnterior == form.getNovoStatus()) {
            throw new IllegalArgumentException("O novo status deve ser diferente do status atual");
        }

        if (form.getNovoStatus() == StatusChamado.CONCLUIDO) {
            LocalDateTime dataFechamento = form.getDataFechamento() != null
                ? form.getDataFechamento()
                : LocalDateTime.now();
            if (c.getDataAbertura() != null && dataFechamento.isBefore(c.getDataAbertura())) {
                throw new IllegalArgumentException("A data de fechamento não pode ser anterior à abertura");
            }
            c.setDataFechamento(dataFechamento);
        } else {
            c.setDataFechamento(null);
            c.setTempoAtendimentoMinutos(null);
        }

        c.setStatus(form.getNovoStatus());
        chamadoRepository.save(c);
        registrarHistorico(c, statusAnterior, c.getStatus(), form.getObservacao());
    }

    /**
     * Exclui primeiro o histórico e depois o chamado.
     *
     * @param id identificador do chamado
     */
    @Transactional
    public void excluir(Long id) {
        if (!chamadoRepository.existsById(id)) {
            throw new EntityNotFoundException("Chamado #" + id + " não encontrado");
        }
        historicoRepository.findByChamadoIdOrderByAlteradoEmDesc(id)
            .forEach(historicoRepository::delete);
        chamadoRepository.deleteById(id);
    }

    // ── Conversão Form → Entidade ─────────────────────────────────────────────

    private Chamado montarEntidade(Chamado c, ChamadoForm form) {
        c.setNumeroCh(form.getNumeroCh());

        Prestador prestador = prestadorRepository.findById(form.getPrestadorId())
            .orElseThrow(() -> new EntityNotFoundException("Prestador não encontrado"));
        c.setPrestador(prestador);

        if (form.getEquipamentoId() != null) {
            Equipamento equipamento = equipamentoRepository.findById(form.getEquipamentoId())
                .orElseThrow(() -> new EntityNotFoundException("Equipamento não encontrado"));
            if (equipamento.getPrestador() == null
                    || !equipamento.getPrestador().getId().equals(prestador.getId())) {
                throw new IllegalArgumentException("O equipamento não pertence ao prestador selecionado");
            }
            c.setEquipamento(equipamento);
        } else {
            c.setEquipamento(null);
        }

        c.setSetor(setorRepository.findById(form.getSetorId())
            .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado")));
        c.setMotivo(motivoRepository.findById(form.getMotivoId())
            .orElseThrow(() -> new EntityNotFoundException("Motivo não encontrado")));

        c.setDescricaoAtendimento(form.getDescricaoAtendimento());
        c.setDataAbertura(form.getDataAbertura());
        c.setConceito(form.getConceito());
        c.setObservacao(form.getObservacao());

        return c;
    }

    private void atualizarOcorrencias(Chamado chamado) {
        if (chamado.getEquipamento() == null) {
            chamado.setOcorrenciasMesmoServico(0);
            return;
        }
        long ocorrencias = chamadoRepository.contarOcorrenciasServico(
            chamado.getEquipamento().getId(),
            chamado.getMotivo().getId(),
            chamado.getPrestador().getId(),
            LocalDateTime.now().minusDays(90),
            chamado.getId() != null ? chamado.getId() : 0L
        );
        chamado.setOcorrenciasMesmoServico((int) (ocorrencias + 1));
    }

    // ── Conversão Entidade → Form ─────────────────────────────────────────────

    /**
     * Converte uma entidade existente para o DTO utilizado no formulário de edição.
     *
     * @param c chamado de origem
     * @return formulário preenchido
     */
    public ChamadoForm toForm(Chamado c) {
        return ChamadoForm.builder()
            .numeroCh(c.getNumeroCh())
            .prestadorId(c.getPrestador() != null ? c.getPrestador().getId() : null)
            .equipamentoId(c.getEquipamento() != null ? c.getEquipamento().getId() : null)
            .setorId(c.getSetor() != null ? c.getSetor().getId() : null)
            .motivoId(c.getMotivo() != null ? c.getMotivo().getId() : null)
            .descricaoAtendimento(c.getDescricaoAtendimento())
            .dataAbertura(c.getDataAbertura())
            .conceito(c.getConceito())
            .observacao(c.getObservacao())
            .build();
    }

    // ── Histórico ─────────────────────────────────────────────────────────────

    private void registrarHistorico(Chamado c, StatusChamado de, StatusChamado para, String obs) {
        ChamadoHistorico hist = ChamadoHistorico.builder()
            .chamado(c)
            .statusDe(de)
            .statusPara(para)
            .observacao(obs)
            .build();
        historicoRepository.save(hist);
    }
}
