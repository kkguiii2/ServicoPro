package com.empresa.controleservico.controller;

import com.empresa.controleservico.dto.MotivoForm;
import com.empresa.controleservico.dto.SetorForm;
import com.empresa.controleservico.entity.Motivo;
import com.empresa.controleservico.entity.Setor;
import com.empresa.controleservico.repository.MotivoRepository;
import com.empresa.controleservico.repository.SetorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controla os cadastros administrativos de setores e motivos de chamado.
 */
@Controller
@RequestMapping("/configuracoes")
@RequiredArgsConstructor
public class ConfiguracoesController {

    private final SetorRepository  setorRepository;
    private final MotivoRepository motivoRepository;

    /**
     * Lista setores e motivos e prepara os respectivos formulários.
     *
     * @param model modelo da view Thymeleaf
     * @return template de configurações
     */
    @GetMapping
    public String configuracoes(Model model) {
        model.addAttribute("setores",    setorRepository.findAllByOrderByNomeAsc());
        model.addAttribute("motivos",    motivoRepository.findAllByOrderByDescricaoAsc());
        if (!model.containsAttribute("novoSetor")) {
            model.addAttribute("novoSetor", new SetorForm());
        }
        if (!model.containsAttribute("novoMotivo")) {
            model.addAttribute("novoMotivo", new MotivoForm());
        }
        model.addAttribute("pageTitle",  "Configurações");
        model.addAttribute("activePage", "configuracoes");
        return "configuracoes/index";
    }

    /**
     * Valida e cadastra um setor ativo.
     *
     * @param form dados do novo setor
     * @param result resultado da validação
     * @param model modelo usado para reapresentar a página
     * @param ra atributos preservados após redirecionamento
     * @return view com erros ou redirecionamento para configurações
     */
    @PostMapping("/setores/salvar")
    public String salvarSetor(
            @Valid @ModelAttribute("novoSetor") SetorForm form,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepararModelo(model, new MotivoForm());
            return "configuracoes/index";
        }
        Setor setor = Setor.builder().nome(form.getNome().trim()).ativo(true).build();
        setorRepository.save(setor);
        ra.addFlashAttribute("successMsg", "Setor salvo!");
        return "redirect:/configuracoes";
    }

    /**
     * Exclui um setor, sujeito às restrições de integridade do banco.
     *
     * @param id identificador do setor
     * @param ra atributos de redirecionamento
     * @return redirecionamento para configurações
     */
    @PostMapping("/setores/{id}/excluir")
    public String excluirSetor(@PathVariable Long id, RedirectAttributes ra) {
        setorRepository.deleteById(id);
        ra.addFlashAttribute("successMsg", "Setor excluído.");
        return "redirect:/configuracoes";
    }

    /**
     * Valida e cadastra um motivo de chamado.
     *
     * @param form dados do novo motivo
     * @param result resultado da validação
     * @param model modelo usado para reapresentar a página
     * @param ra atributos preservados após redirecionamento
     * @return view com erros ou redirecionamento para configurações
     */
    @PostMapping("/motivos/salvar")
    public String salvarMotivo(
            @Valid @ModelAttribute("novoMotivo") MotivoForm form,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepararModelo(model, new SetorForm());
            return "configuracoes/index";
        }
        Motivo motivo = Motivo.builder().descricao(form.getDescricao().trim()).build();
        motivoRepository.save(motivo);
        ra.addFlashAttribute("successMsg", "Motivo salvo!");
        return "redirect:/configuracoes";
    }

    /**
     * Exclui um motivo, sujeito às restrições de integridade do banco.
     *
     * @param id identificador do motivo
     * @param ra atributos de redirecionamento
     * @return redirecionamento para configurações
     */
    @PostMapping("/motivos/{id}/excluir")
    public String excluirMotivo(@PathVariable Long id, RedirectAttributes ra) {
        motivoRepository.deleteById(id);
        ra.addFlashAttribute("successMsg", "Motivo excluído.");
        return "redirect:/configuracoes";
    }

    private void prepararModelo(Model model, Object outroFormulario) {
        model.addAttribute("setores", setorRepository.findAllByOrderByNomeAsc());
        model.addAttribute("motivos", motivoRepository.findAllByOrderByDescricaoAsc());
        if (outroFormulario instanceof MotivoForm motivoForm) {
            model.addAttribute("novoMotivo", motivoForm);
        } else {
            model.addAttribute("novoSetor", outroFormulario);
        }
        model.addAttribute("pageTitle", "Configurações");
        model.addAttribute("activePage", "configuracoes");
    }
}
