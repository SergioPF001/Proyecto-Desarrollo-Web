package com.amsuno.service;

import com.amsuno.model.Cliente;
import com.amsuno.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) { this.repo = repo; }

    public List<Cliente> listar() { return repo.findAll(); }
    public void agregar(Cliente c) { repo.save(c); }
    public Cliente buscar(Long id) { return repo.findById(id).orElse(null); }
    public void eliminar(Long id) { repo.deleteById(id); }
}
