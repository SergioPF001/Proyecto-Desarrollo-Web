package com.amsuno.service;

import com.amsuno.model.Cliente;
import com.amsuno.model.Pelicula;
import com.amsuno.model.Reserva;
import com.amsuno.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository repo;

    public ReservaService(ReservaRepository repo) { this.repo = repo; }

    public List<Reserva> listar() { return repo.findAll(); }

    public void agregar(Cliente cliente, Pelicula pelicula, String fecha,
                        int asientos, double total, String estado) {
        repo.save(new Reserva(cliente, pelicula, fecha, asientos, total, estado));
    }

    public Reserva buscar(Long id) { return repo.findById(id).orElse(null); }
    public void eliminar(Long id) { repo.deleteById(id); }

    public void confirmar(Long id) {
        repo.findById(id).ifPresent(r -> {
            r.setEstado("Confirmada");
            repo.save(r);
        });
    }
}
