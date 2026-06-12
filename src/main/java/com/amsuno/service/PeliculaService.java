package com.amsuno.service;

import com.amsuno.model.Pelicula;
import com.amsuno.repository.PeliculaRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PeliculaService {

    private final PeliculaRepository repo;

    public PeliculaService(PeliculaRepository repo) { this.repo = repo; }

    public List<Pelicula> listar() { return repo.findAll(); }
    public void agregar(Pelicula p) { repo.save(p); }
    public Pelicula buscar(Long id) { return repo.findById(id).orElse(null); }
    public void eliminar(Long id) { repo.deleteById(id); }

    public List<String> listarGeneros() {
        return repo.findAll().stream()
                .map(Pelicula::getGenero)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
