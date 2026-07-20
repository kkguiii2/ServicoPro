package com.empresa.controleservico.controller;

import com.empresa.controleservico.dto.EquipamentoForm;
import com.empresa.controleservico.service.EquipamentoService;
import com.empresa.controleservico.service.PrestadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controla as operações administrativas de consulta, cadastro e exclusão de equipamentos.
 */
@Controller
@RequestMapping("/equipamentos")
@RequiredArgsConstructor
public class EquipamentoController {

    private final EquipamentoService equipamentoService;
    private final PrestadorService   prestadorService;

    /**
     * Lista equipamentos, opcionalmente limitados a um prestador.
     *
     * @param prestadorId identificador opcional do prestador
     * @param model modelo da view Thymeleaf
     * @return template de equipamentos
     */
    @GetMapping
    public String listar(@RequestParam(required = false) Long prestadorId, Model model) {
        model.addAttribute("equipamentos",
            prestadorId != null
                ? equipamentoService.listarPorPrestador(prestadorId, null)
                : equipamentoService.listarTodos()
        );
        model.addAttribute("prestadores",    prestadorService.listarAtivos());
        model.addAttribute("filtPrestadorId",prestadorId);
        if (!model.containsAttribute("novoEquipamento")) {
            model.addAttribute("novoEquipamento", new EquipamentoForm());
        }
        model.addAttribute("pageTitle",  "Equipamentos");
        model.addAttribute("activePage", "equipamentos");
        return "equipamentos/lista";
    }

    /**
     * Valida e persiste um equipamento associado a um prestador.
     *
     * @param form dados submetidos pelo formulário
     * @param result resultado da validação
     * @param ra atributos de redirecionamento
     * @param model modelo utilizado ao reapresentar erros
     * @return view com erros ou redirecionamento para a listagem
     */
    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute("novoEquipamento") EquipamentoForm form,
            BindingResult result,
            RedirectAttributes ra,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("equipamentos", equipamentoService.listarTodos());
            model.addAttribute("prestadores",  prestadorService.listarAtivos());
            model.addAttribute("pageTitle",   "Equipamentos");
            model.addAttribute("activePage",  "equipamentos");
            return "equipamentos/lista";
        }

        equipamentoService.salvar(form);
        ra.addFlashAttribute("successMsg", "Equipamento salvo com sucesso!");
        return "redirect:/equipamentos";
    }

    /**
     * Exclui um equipamento pelo identificador.
     *
     * @param id identificador do equipamento
     * @param ra atributos de redirecionamento
     * @return redirecionamento para a listagem
     */
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        equipamentoService.excluir(id);
        ra.addFlashAttribute("successMsg", "Equipamento excluído.");
        return "redirect:/equipamentos";
    }
}
