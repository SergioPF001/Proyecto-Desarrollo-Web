package com.amsuno.controller.api;

import com.amsuno.dto.PeliculaDTO;
import com.amsuno.exception.RecursoNoEncontradoException;
import com.amsuno.model.Pelicula;
import com.amsuno.service.PeliculaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/peliculas")
public class PeliculaApiController {

    private final PeliculaService peliculaService;

    public PeliculaApiController(PeliculaService peliculaService) {
        this.peliculaService = peliculaService;
    }

    @GetMapping
    public List<PeliculaDTO> listar(@RequestParam(required = false) String genero) {
        return peliculaService.listar().stream()
                .filter(p -> genero == null || genero.isBlank() || genero.equalsIgnoreCase(p.getGenero()))
                .map(PeliculaDTO::new)
                .toList();
    }

    @GetMapping("/generos")
    public List<String> generos() {
        return peliculaService.listarGeneros();
    }

    @GetMapping("/{id}")
    public PeliculaDTO obtener(@PathVariable Long id) {
        Pelicula pelicula = peliculaService.buscar(id);
        if (pelicula == null) {
            throw new RecursoNoEncontradoException("No existe la película con id " + id);
        }
        return new PeliculaDTO(pelicula);
    }
}
