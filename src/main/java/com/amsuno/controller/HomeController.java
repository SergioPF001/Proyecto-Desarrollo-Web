package com.amsuno.controller;

import com.amsuno.service.PeliculaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final PeliculaService peliculaService;

    public HomeController(PeliculaService peliculaService) {
        this.peliculaService = peliculaService;
    }

    @GetMapping("/")
    public String inicio(Model modelo) {
        List<String> imagenes = List.of(
            "https://images.unsplash.com/photo-1761948245185-fc300ad20316?w=400",
            "https://images.unsplash.com/photo-1765510296004-614b6cc204da?w=400",
            "https://images.unsplash.com/photo-1773353681034-3736f0fde648?w=400",
            "https://images.unsplash.com/photo-1759230766134-e3ff1c27d20e?w=400"
        );
        modelo.addAttribute("peliculas", peliculaService.listar());
        modelo.addAttribute("imagenes",  imagenes);
        return "index";
    }
}
