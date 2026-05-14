package com.amsuno.service;

import com.amsuno.model.Reserva;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReservaService {

    private final List<Reserva> reservas = new ArrayList<>();
    private final AtomicLong contador = new AtomicLong();

    public ReservaService() {
        reservas.add(new Reserva(contador.incrementAndGet(), "Carlos Mendoza", "Avatar 3: El Semillero",         "2026-04-18 19:00", 2, 5000.00,  "Confirmada"));
        reservas.add(new Reserva(contador.incrementAndGet(), "María García",   "Dune: Parte Tres",               "2026-04-19 21:00", 4, 10000.00, "Pendiente"));
        reservas.add(new Reserva(contador.incrementAndGet(), "José Rodríguez", "Guardianes de la Galaxia Vol. 4","2026-04-20 16:30", 3, 7500.00,  "Confirmada"));
    }

    public List<Reserva> listar() {
        return reservas;
    }

    public void agregar(Reserva reserva) {
        reserva.setId(contador.incrementAndGet());
        reservas.add(reserva);
    }

    public Reserva buscar(Long id) {
        return reservas.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void eliminar(Long id) {
        reservas.removeIf(r -> r.getId().equals(id));
    }

    public void confirmar(Long id) {
        reservas.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .ifPresent(r -> r.setEstado("Confirmada"));
    }
}
