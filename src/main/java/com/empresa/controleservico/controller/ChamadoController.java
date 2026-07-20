package com.empresa.controleservico.controller;

import com.empresa.controleservico.dto.AlterarStatusForm;
import com.empresa.controleservico.dto.ChamadoForm;
import com.empresa.controleservico.entity.Chamado;
import com.empresa.controleservico.entity.ChamadoHistorico;
import com.empresa.controleservico.entity.Equipamento;
import com.empresa.controleservico.entity.Prestador;
import com.empresa.controleservico.enums.ConceitoAvaliacao;
import com.empresa.controleservico.enums.StatusChamado;
import com.empresa.controleservico.repository.ChamadoHistoricoRepository;
import com.empresa.controleservico.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Controla o ciclo de vida dos chamados nas páginas MVC, incluindo consulta,
 * criação, edição, detalhe, alteração de status e exclusão administrativa.
 */
@Controller
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService              chamadoService;
    private final PrestadorService            prestadorService;
    private final EquipamentoService          equipamentoService;
    private final ChamadoHistoricoRepository  historicoRepository;
    private final com.empresa.controleservico.repository.SetorRepository   setorRepository;
    private final com.empresa.controleservico.repository.MotivoRepository  motivoRepository;

    // ── Lista com filtros e paginação ────────────────────────────────────────

    /**
     * Lista chamados com filtros, paginação e ordenação validados pela camada de serviço.
     * Datas informadas são convertidas para um intervalo semiaberto, incluindo todo o dia final.
     *
     * @param prestadorId prestador opcional
     * @param status estado opcional
     * @param setorId setor opcional
     * @param motivoId motivo opcional
     * @param equipamentoId equipamento opcional
     * @param dataInicio primeiro dia incluído
     * @param dataFim último dia incluído
     * @param conceito avaliação opcional
     * @param numeroCh trecho opcional do número externo
     * @param page índice de página iniciado em zero
     * @param size tamanho solicitado da página
     * @param sortBy propriedade de ordenação
     * @param sortDir direção de ordenação
     * @param model modelo da view Thymeleaf
     * @return template da listagem de chamados
     */
    @GetMapping
    public String listar(
            @RequestParam(required = false) Long prestadorId,
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long motivoId,
            @RequestParam(required = false) Long equipamentoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) ConceitoAvaliacao conceito,
            @RequestParam(required = false) String numeroCh,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dataAbertura") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial");
        }
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fimExclusive = dataFim != null ? dataFim.plusDays(1).atStartOfDay() : null;
        Page<Chamado> chamados = chamadoService.listar(
            prestadorId, status, setorId, motivoId, equipamentoId,
            inicio, fimExclusive, conceito, numeroCh, page, size, sortBy, sortDir
        );

        model.addAttribute("chamados",    chamados);
        model.addAttribute("prestadores", prestadorService.listarAtivos());
        model.addAttribute("setores",     setorRepository.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("motivos",     motivoRepository.findAllByOrderByDescricaoAsc());
        model.addAttribute("equipamentos",equipamentoService.listarAtivos());
        model.addAttribute("statusList",  StatusChamado.values());
        model.addAttribute("conceitoList",ConceitoAvaliacao.values());

        // Filtros para manter no form
        model.addAttribute("filtPrestadorId",   prestadorId);
        model.addAttribute("filtStatus",        status);
        model.addAttribute("filtSetorId",       setorId);
        model.addAttribute("filtMotivoId",      motivoId);
        model.addAttribute("filtEquipamentoId", equipamentoId);
        model.addAttribute("filtDataInicio",    dataInicio);
        model.addAttribute("filtDataFim",       dataFim);
        model.addAttribute("filtConceito",      conceito);
        model.addAttribute("filtNumeroCh",      numeroCh);
        model.addAttribute("sortBy",   sortBy);
        model.addAttribute("sortDir",  sortDir);

        model.addAttribute("pageTitle",  "Chamados");
        model.addAttribute("activePage", "chamados");

        return "chamados/lista";
    }

    // ── Formulário de criação ────────────────────────────────────────────────

    /**
     * Prepara o formulário para abertura de um chamado.
     *
     * @param model modelo da view Thymeleaf
     * @return template do formulário
     */
    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("form",        new ChamadoForm());
        model.addAttribute("editando",   false);
        model.addAttribute("prestadores", prestadorService.listarAtivos());
        model.addAttribute("setores",     setorRepository.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("motivos",     motivoRepository.findAllByOrderByDescricaoAsc());
        model.addAttribute("equipamentos",equipamentoService.listarAtivos());
        model.addAttribute("conceitoList",ConceitoAvaliacao.values());
        model.addAttribute("pageTitle",  "Novo Chamado");
        model.addAttribute("activePage", "chamados");
        return "chamados/form";
    }

    /**
     * Valida e cria um chamado inicialmente aberto.
     *
     * @param form dados do chamado
     * @param result resultado da validação
     * @param model modelo utilizado ao reapresentar erros
     * @param ra atributos preservados após redirecionamento
     * @return formulário com erros ou redirecionamento para o detalhe
     */
    @PostMapping("/novo")
    public String criar(
            @Valid @ModelAttribute("form") ChamadoForm form,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("prestadores", prestadorService.listarAtivos());
            model.addAttribute("editando",   false);
            model.addAttribute("setores",     setorRepository.findByAtivoTrueOrderByNomeAsc());
            model.addAttribute("motivos",     motivoRepository.findAllByOrderByDescricaoAsc());
            model.addAttribute("equipamentos",equipamentoService.listarAtivos());
            model.addAttribute("conceitoList",ConceitoAvaliacao.values());
            model.addAttribute("pageTitle",  "Novo Chamado");
            model.addAttribute("activePage", "chamados");
            return "chamados/form";
        }

        Chamado c = chamadoService.criar(form);
        ra.addFlashAttribute("successMsg", "Chamado #" + c.getId() + " criado com sucesso!");
        return "redirect:/chamados/" + c.getId();
    }

    // ── Formulário de edição ─────────────────────────────────────────────────

    /**
     * Prepara a edição, preservando associações atuais que estejam inativas.
     *
     * @param id identificador do chamado
     * @param model modelo da view Thymeleaf
     * @return template do formulário em modo de edição
     */
    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        Chamado chamado = chamadoService.buscarPorId(id);
        model.addAttribute("form",        chamadoService.toForm(chamado));
        model.addAttribute("chamado",     chamado);
        model.addAttribute("editando",    true);
        model.addAttribute("chamadoId",   id);
        model.addAttribute("prestadores", prestadoresParaEdicao(chamado));
        model.addAttribute("setores",     setorRepository.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("motivos",     motivoRepository.findAllByOrderByDescricaoAsc());
        model.addAttribute("equipamentos", equipamentosParaEdicao(chamado));
        model.addAttribute("conceitoList",ConceitoAvaliacao.values());
        model.addAttribute("pageTitle",  "Editar Chamado #" + id);
        model.addAttribute("activePage", "chamados");
        return "chamados/form";
    }

    /**
     * Valida e atualiza os dados editáveis de um chamado.
     *
     * @param id identificador do chamado
     * @param form dados atualizados
     * @param result resultado da validação
     * @param model modelo utilizado ao reapresentar erros
     * @param ra atributos de redirecionamento
     * @return formulário com erros ou redirecionamento para o detalhe
     */
    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ChamadoForm form,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            Chamado chamado = chamadoService.buscarPorId(id);
            model.addAttribute("chamado",     chamado);
            model.addAttribute("editando",    true);
            model.addAttribute("chamadoId",   id);
            model.addAttribute("prestadores", prestadoresParaEdicao(chamado));
            model.addAttribute("setores",     setorRepository.findByAtivoTrueOrderByNomeAsc());
            model.addAttribute("motivos",     motivoRepository.findAllByOrderByDescricaoAsc());
            model.addAttribute("equipamentos", equipamentosParaEdicao(chamado));
            model.addAttribute("conceitoList",ConceitoAvaliacao.values());
            model.addAttribute("pageTitle",  "Editar Chamado #" + id);
            model.addAttribute("activePage", "chamados");
            return "chamados/form";
        }

        chamadoService.editar(id, form);
        ra.addFlashAttribute("successMsg", "Chamado #" + id + " atualizado com sucesso!");
        return "redirect:/chamados/" + id;
    }

    // ── Detalhe ──────────────────────────────────────────────────────────────

    /**
     * Exibe os dados completos e o histórico de mudanças de status.
     *
     * @param id identificador do chamado
     * @param model modelo da view Thymeleaf
     * @return template de detalhe
     */
    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Chamado chamado = chamadoService.buscarPorId(id);
        List<ChamadoHistorico> historico = historicoRepository
            .findByChamadoIdOrderByAlteradoEmDesc(id);

        model.addAttribute("chamado",   chamado);
        model.addAttribute("historico", historico);
        model.addAttribute("statusForm",new AlterarStatusForm());
        model.addAttribute("statusList",StatusChamado.values());
        model.addAttribute("pageTitle", "Chamado #" + id);
        model.addAttribute("activePage","chamados");
        return "chamados/detalhe";
    }

    // ── Alterar Status ───────────────────────────────────────────────────────

    /**
     * Altera o status e registra a transição no histórico.
     *
     * @param id identificador do chamado
     * @param form novo status, observação e fechamento opcional
     * @param result resultado da validação
     * @param ra atributos de redirecionamento
     * @return redirecionamento para o detalhe
     */
    @PostMapping("/{id}/status")
    public String alterarStatus(
            @PathVariable Long id,
            @Valid @ModelAttribute("statusForm") AlterarStatusForm form,
            BindingResult result,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Preencha todos os campos obrigatórios.");
            return "redirect:/chamados/" + id;
        }

        chamadoService.alterarStatus(id, form);
        ra.addFlashAttribute("successMsg", "Status do chamado #" + id + " atualizado!");
        return "redirect:/chamados/" + id;
    }

    // ── Exclusão ─────────────────────────────────────────────────────────────

    /**
     * Exclui o chamado e seu histórico; a autorização administrativa é aplicada pelo Spring Security.
     *
     * @param id identificador do chamado
     * @param ra atributos de redirecionamento
     * @return redirecionamento para a listagem
     */
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        chamadoService.excluir(id);
        ra.addFlashAttribute("successMsg", "Chamado #" + id + " excluído.");
        return "redirect:/chamados";
    }

    private List<Prestador> prestadoresParaEdicao(Chamado chamado) {
        List<Prestador> prestadores = new ArrayList<>(prestadorService.listarAtivos());
        Prestador atual = chamado.getPrestador();
        if (atual != null && prestadores.stream().noneMatch(p -> p.getId().equals(atual.getId()))) {
            prestadores.add(atual);
            prestadores.sort(Comparator.comparing(Prestador::getNome, String.CASE_INSENSITIVE_ORDER));
        }
        return prestadores;
    }

    private List<Equipamento> equipamentosParaEdicao(Chamado chamado) {
        List<Equipamento> equipamentos = new ArrayList<>(equipamentoService.listarAtivos());
        Equipamento atual = chamado.getEquipamento();
        if (atual != null && equipamentos.stream().noneMatch(e -> e.getId().equals(atual.getId()))) {
            equipamentos.add(atual);
            equipamentos.sort(Comparator.comparing(Equipamento::getNome, String.CASE_INSENSITIVE_ORDER));
        }
        return equipamentos;
    }
}
