package com.amsuno;

import com.amsuno.model.*;
import com.amsuno.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataLoader implements CommandLineRunner {

    private final ClienteRepository         clientes;
    private final PeliculaRepository        peliculas;
    private final ReservaRepository         reservas;
    private final SnackRepository           snacks;
    private final SalaRepository            salas;
    private final AsientoRepository         asientos;
    private final ReservaAsientoRepository  reservaAsientosRepo;

    public DataLoader(ClienteRepository clientes, PeliculaRepository peliculas,
                      ReservaRepository reservas, SnackRepository snacks,
                      SalaRepository salas, AsientoRepository asientos,
                      ReservaAsientoRepository reservaAsientosRepo) {
        this.clientes            = clientes;
        this.peliculas           = peliculas;
        this.reservas            = reservas;
        this.snacks              = snacks;
        this.salas               = salas;
        this.asientos            = asientos;
        this.reservaAsientosRepo = reservaAsientosRepo;
    }

    @Override
    public void run(String... args) {
        inicializarSalas();

        if (clientes.count() > 0) {
            repararAsientosDemoData();
            return;
        }

        inicializarDemostracion();
    }

    private void inicializarSalas() {
        if (salas.count() > 0) return;

        Sala sala1 = salas.save(new Sala("Sala Principal", 5, 8));
        Sala sala2 = salas.save(new Sala("Sala VIP",       4, 6));

        String[] filasSala1 = {"A", "B", "C", "D", "E"};
        for (String fila : filasSala1) {
            String tipo = (fila.equals("C") || fila.equals("D")) ? "preferencial" : "normal";
            for (int num = 1; num <= 8; num++) {
                asientos.save(new Asiento(sala1, fila, num, tipo));
            }
        }

        String[] filasSala2 = {"A", "B", "C", "D"};
        for (String fila : filasSala2) {
            for (int num = 1; num <= 6; num++) {
                asientos.save(new Asiento(sala2, fila, num, "vip"));
            }
        }

        for (Pelicula p : peliculas.findAll()) {
            if (p.getSala() == null) {
                p.setSala(sala1);
                peliculas.save(p);
            }
        }
    }

    private void repararAsientosDemoData() {
        Map<Long, List<Asiento>> cacheSala   = new HashMap<>();
        Map<Long, Integer>       offsetSala  = new HashMap<>();

        for (Reserva r : reservas.findAll()) {
            if (!reservaAsientosRepo.findByReservaId(r.getId()).isEmpty()) continue;
            if (r.getAsientos() <= 0 || r.getPelicula() == null || r.getPelicula().getSala() == null) continue;

            Long salaId = r.getPelicula().getSala().getId();

            if (!cacheSala.containsKey(salaId)) {
                cacheSala.put(salaId,  asientos.findBySalaId(salaId));
                offsetSala.put(salaId, 0);
            }

            List<Asiento> lista = cacheSala.get(salaId);
            int desde = offsetSala.get(salaId);
            int hasta = Math.min(desde + r.getAsientos(), lista.size());

            for (int i = desde; i < hasta; i++) {
                reservaAsientosRepo.save(new ReservaAsiento(r, lista.get(i)));
            }
            offsetSala.put(salaId, hasta);
        }
    }

    private void inicializarDemostracion() {
        Sala sala1 = salas.findAll().get(0);
        Sala sala2 = salas.findAll().get(1);

        Cliente c1 = clientes.save(new Cliente("Carlos Mendoza", "carlos@email.com", "08012345678"));
        Cliente c2 = clientes.save(new Cliente("María García",   "maria@email.com",  "08098765432"));
        Cliente c3 = clientes.save(new Cliente("José Rodríguez", "jose@email.com",   "08056781234"));

        Pelicula p1 = new Pelicula("Avatar 3: El Semillero",          "Ciencia Ficción", 180, "PG-13", 2500.0, true,  "14:00,17:30,21:00");
        p1.setSala(sala1);
        p1 = peliculas.save(p1);

        Pelicula p2 = new Pelicula("Guardianes de la Galaxia Vol. 4", "Acción",          150, "PG-13", 2500.0, true,  "13:00,16:00,19:30");
        p2.setSala(sala2);
        p2 = peliculas.save(p2);

        Pelicula p3 = new Pelicula("Aventura en el Bosque Mágico",    "Animación",       105, "G",     2000.0, false, "12:00,14:30,17:00");
        p3.setSala(sala1);
        peliculas.save(p3);

        Pelicula p4 = new Pelicula("Dune: Parte Tres",                "Ciencia Ficción", 165, "PG-13", 2500.0, true,  "15:00,18:30,22:00");
        p4.setSala(sala1);
        p4 = peliculas.save(p4);

        List<Asiento> asientosSala1 = asientos.findBySalaId(sala1.getId());
        List<Asiento> asientosSala2 = asientos.findBySalaId(sala2.getId());

        Reserva r1 = reservas.save(new Reserva(c1, p1, "2026-04-18 19:00", 2,  5000.0,  "Confirmada"));
        reservaAsientosRepo.save(new ReservaAsiento(r1, asientosSala1.get(0)));
        reservaAsientosRepo.save(new ReservaAsiento(r1, asientosSala1.get(1)));

        Reserva r2 = reservas.save(new Reserva(c2, p4, "2026-04-19 21:00", 4, 10000.0,  "Pendiente"));
        reservaAsientosRepo.save(new ReservaAsiento(r2, asientosSala1.get(2)));
        reservaAsientosRepo.save(new ReservaAsiento(r2, asientosSala1.get(3)));
        reservaAsientosRepo.save(new ReservaAsiento(r2, asientosSala1.get(4)));
        reservaAsientosRepo.save(new ReservaAsiento(r2, asientosSala1.get(5)));

        Reserva r3 = reservas.save(new Reserva(c3, p2, "2026-04-20 16:30", 3,  7500.0,  "Confirmada"));
        reservaAsientosRepo.save(new ReservaAsiento(r3, asientosSala2.get(0)));
        reservaAsientosRepo.save(new ReservaAsiento(r3, asientosSala2.get(1)));
        reservaAsientosRepo.save(new ReservaAsiento(r3, asientosSala2.get(2)));

        snacks.save(new Snack("Palomitas Grandes", "Snacks",  1500.0, 100, "Palomitas de maíz grandes"));
        snacks.save(new Snack("Refresco",           "Bebidas",  500.0, 150, "Refresco de cola"));
        snacks.save(new Snack("Combo Familiar",     "Combos",  3500.0,  50, "2 Palomitas + 2 Refrescos"));
    }
}
