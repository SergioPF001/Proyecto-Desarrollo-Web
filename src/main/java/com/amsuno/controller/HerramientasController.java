package com.amsuno.controller;

import com.amsuno.exception.ConsultaExternaException;
import com.amsuno.service.ConsultaExternaService;
import com.amsuno.service.PeliculaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/herramientas")
public class HerramientasController {

    private final ConsultaExternaService consultaExternaService;
    private final PeliculaService peliculaService;

    public HerramientasController(ConsultaExternaService consultaExternaService,
                                  PeliculaService peliculaService) {
        this.consultaExternaService = consultaExternaService;
        this.peliculaService        = peliculaService;
    }

    @GetMapping
    public String herramientas(@RequestParam(required = false) String dni, Model modelo) {
        modelo.addAttribute("peliculas", peliculaService.listar());

        try {
            modelo.addAttribute("tipoCambio", consultaExternaService.tipoCambio());
        } catch (ConsultaExternaException e) {
            modelo.addAttribute("errorTipoCambio", e.getMessage());
        }

        if (dni != null && !dni.isBlank()) {
            try {
                modelo.addAttribute("persona", consultaExternaService.consultarDni(dni));
            } catch (ConsultaExternaException e) {
                modelo.addAttribute("errorDni", e.getMessage());
            }
        }

        modelo.addAttribute("dni", dni != null ? dni : "");
        return "admin/herramientas";
    }
}
