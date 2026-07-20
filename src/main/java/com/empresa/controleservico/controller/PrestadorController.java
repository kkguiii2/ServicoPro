package com.empresa.controleservico.controller;

import com.empresa.controleservico.dto.PrestadorForm;
import com.empresa.controleservico.service.PrestadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controla as telas administrativas de cadastro e ativação de prestadores.
 */
@Controller
@RequestMapping("/prestadores")
@RequiredArgsConstructor
public class PrestadorController {

    private final PrestadorService prestadorService;

    /**
     * Lista todos os prestadores e prepara o formulário de cadastro.
     *
     * @param model modelo da view Thymeleaf
     * @return template de prestadores
     */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("prestadores",  prestadorService.listarTodos());
        if (!model.containsAttribute("novoPrestador")) {
            model.addAttribute("novoPrestador", new PrestadorForm());
        }
        model.addAttribute("pageTitle",   "Prestadores");
        model.addAttribute("activePage",  "prestadores");
        return "prestadores/lista";
    }

    /**
     * Valida e persiste um novo prestador.
     *
     * @param form dados submetidos pelo formulário
     * @param result resultado da validação Jakarta Bean Validation
     * @param model modelo utilizado ao reapresentar erros
     * @param ra atributos preservados após redirecionamento
     * @return view com erros ou redirecionamento para a listagem
     */
    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute("novoPrestador") PrestadorForm form,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("prestadores", prestadorService.listarTodos());
            model.addAttribute("pageTitle", "Prestadores");
            model.addAttribute("activePage", "prestadores");
            return "prestadores/lista";
        }
        prestadorService.salvar(form);
        ra.addFlashAttribute("successMsg", "Prestador salvo com sucesso!");
        return "redirect:/prestadores";
    }

    /**
     * Desativa logicamente um prestador.
     *
     * @param id identificador do prestador
     * @param ra atributos de redirecionamento
     * @return redirecionamento para a listagem
     */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes ra) {
        prestadorService.desativar(id);
        ra.addFlashAttribute("successMsg", "Prestador desativado.");
        return "redirect:/prestadores";
    }

    /**
     * Reativa um prestador previamente desativado.
     *
     * @param id identificador do prestador
     * @param ra atributos de redirecionamento
     * @return redirecionamento para a listagem
     */
    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes ra) {
        prestadorService.ativar(id);
        ra.addFlashAttribute("successMsg", "Prestador ativado.");
        return "redirect:/prestadores";
    }
}
