package com.amsuno.service;

import com.amsuno.model.Snack;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SnackService {

    private final List<Snack> snacks = new ArrayList<>();
    private final AtomicLong contador = new AtomicLong();

    public SnackService() {
        snacks.add(new Snack(contador.incrementAndGet(), "Palomitas Grandes", "Snacks",  1500, 100, "Palomitas de maíz grandes"));
        snacks.add(new Snack(contador.incrementAndGet(), "Refresco",          "Bebidas",  500, 150, "Refresco de cola"));
        snacks.add(new Snack(contador.incrementAndGet(), "Combo Familiar",    "Combos",  3500,  50, "2 Palomitas Grandes + 2 Refrescos"));
    }

    public List<Snack> listar() {
        return snacks;
    }

    public void agregar(Snack snack) {
        snack.setId(contador.incrementAndGet());
        snacks.add(snack);
    }

    public Snack buscar(Long id) {
        return snacks.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void eliminar(Long id) {
        snacks.removeIf(s -> s.getId().equals(id));
    }
}
