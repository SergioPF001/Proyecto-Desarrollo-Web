package com.amsuno.service;

import com.amsuno.model.Pelicula;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PeliculaService {

    private final List<Pelicula> peliculas = new ArrayList<>();
    private final AtomicLong contador = new AtomicLong();

    public PeliculaService() {
        peliculas.add(new Pelicula(contador.incrementAndGet(), "Avatar 3: El Semillero",         "Ciencia Ficción",         180, "PG-13", 2500.00, true,  "14:00,17:30,21:00"));
        peliculas.add(new Pelicula(contador.incrementAndGet(), "Guardianes de la Galaxia Vol. 4", "Acción / Aventura",       150, "PG-13", 2500.00, true,  "13:00,16:00,19:30"));
        peliculas.add(new Pelicula(contador.incrementAndGet(), "Aventura en el Bosque Mágico",    "Animación / Familia",     105, "G",     2000.00, false, "12:00,14:30,17:00"));
        peliculas.add(new Pelicula(contador.incrementAndGet(), "Dune: Parte Tres",                "Ciencia Ficción / Drama", 165, "PG-13", 2500.00, true,  "15:00,18:30,22:00"));
    }

    public List<Pelicula> listar() {
        return peliculas;
    }

    public void agregar(Pelicula pelicula) {
        pelicula.setId(contador.incrementAndGet());
        peliculas.add(pelicula);
    }

    public Pelicula buscar(Long id) {
        return peliculas.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void eliminar(Long id) {
        peliculas.removeIf(p -> p.getId().equals(id));
    }
}
