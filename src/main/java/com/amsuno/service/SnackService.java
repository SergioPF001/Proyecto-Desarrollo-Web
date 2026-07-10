package com.amsuno.service;

import com.amsuno.exception.RecursoNoEncontradoException;
import com.amsuno.model.Snack;
import com.amsuno.repository.SnackRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SnackService {

    private final SnackRepository repo;

    public SnackService(SnackRepository repo) { this.repo = repo; }

    public List<Snack> listar() { return repo.findAll(); }

    public Snack buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el snack con id " + id));
    }

    public Snack agregar(Snack snack) { return repo.save(snack); }

    public Snack actualizar(Long id, Snack datos) {
        Snack snack = buscar(id);
        snack.setNombre(datos.getNombre());
        snack.setCategoria(datos.getCategoria());
        snack.setPrecio(datos.getPrecio());
        snack.setStock(datos.getStock());
        snack.setDescripcion(datos.getDescripcion());
        return repo.save(snack);
    }

    public void eliminar(Long id) { repo.delete(buscar(id)); }
}
