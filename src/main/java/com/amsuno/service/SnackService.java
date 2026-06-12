package com.amsuno.service;

import com.amsuno.model.Snack;
import com.amsuno.repository.SnackRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SnackService {

    private final SnackRepository repo;

    public SnackService(SnackRepository repo) { this.repo = repo; }

    public List<Snack> listar() { return repo.findAll(); }
    public void agregar(Snack s) { repo.save(s); }
    public Snack buscar(Long id) { return repo.findById(id).orElse(null); }
    public void eliminar(Long id) { repo.deleteById(id); }
}
