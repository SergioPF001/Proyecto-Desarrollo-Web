package com.amsuno.service;

import com.amsuno.model.Sala;
import com.amsuno.repository.SalaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SalaService {

    private final SalaRepository repo;

    public SalaService(SalaRepository repo) { this.repo = repo; }

    public List<Sala> listar() { return repo.findAll(); }

    public Sala buscar(Long id) { return repo.findById(id).orElse(null); }

    public void agregar(Sala sala) { repo.save(sala); }
}
