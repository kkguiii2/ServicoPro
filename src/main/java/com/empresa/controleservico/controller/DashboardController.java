package com.empresa.controleservico.controller;

import com.empresa.controleservico.dto.DashboardKpiDto;
import com.empresa.controleservico.service.DashboardService;
import com.empresa.controleservico.service.PrestadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renderiza o dashboard consolidado e aplica o filtro opcional por prestador.
 */
@Controller
@RequestMapping
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final PrestadorService prestadorService;

    /**
     * Monta a visão principal com KPIs, gráficos e chamados recentes.
     *
     * @param prestadorId identificador opcional do prestador a filtrar
     * @param model modelo da view Thymeleaf
     * @return template do dashboard
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(required = false) Long prestadorId,
            Model model) {

        DashboardKpiDto kpis = dashboardService.calcularKpis(prestadorId);
        model.addAttribute("kpis",       kpis);
        model.addAttribute("pageTitle",  "Dashboard");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("prestadorFiltro", prestadorId);
        model.addAttribute("prestadores", prestadorService.listarAtivos());

        return "dashboard/index";
    }
}
