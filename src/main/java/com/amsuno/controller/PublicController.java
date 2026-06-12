package com.amsuno.controller;

import com.amsuno.model.Cliente;
import com.amsuno.model.Pelicula;
import com.amsuno.repository.ClienteRepository;
import com.amsuno.service.AsientoService;
import com.amsuno.service.PeliculaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PublicController {

    private final PeliculaService peliculaService;
    private final AsientoService asientoService;
    private final ClienteRepository clienteRepo;

    public PublicController(PeliculaService peliculaService,
                            AsientoService asientoService,
                            ClienteRepository clienteRepo) {
        this.peliculaService = peliculaService;
        this.asientoService  = asientoService;
        this.clienteRepo     = clienteRepo;
    }

    @GetMapping("/reservar")
    public String formulario(@RequestParam(required = false) Long peliculaId,
                             @RequestParam(required = false) String fecha,
                             @RequestParam(required = false, defaultValue = "false") boolean exito,
                             Model modelo) {

        modelo.addAttribute("peliculas", peliculaService.listar());
        modelo.addAttribute("exito", exito);

        if (peliculaId != null) {
            Pelicula pelicula = peliculaService.buscar(peliculaId);
            if (pelicula != null) {
                modelo.addAttribute("pelicula", pelicula);

                List<String> horarios = pelicula.getHorarios() != null
                        ? Arrays.asList(pelicula.getHorarios().split(","))
                        : new ArrayList<>();
                modelo.addAttribute("horarios", horarios);

                if (fecha != null && !fecha.isBlank() && pelicula.getSala() != null) {
                    List<Map<String, Object>> asientos = asientoService.asientosDisponibles(peliculaId, fecha);
                    Map<String, List<Map<String, Object>>> mapFila = new LinkedHashMap<>();
                    for (Map<String, Object> a : asientos) {
                        mapFila.computeIfAbsent(a.get("fila").toString(), k -> new ArrayList<>()).add(a);
                    }
                    modelo.addAttribute("fecha", fecha);
                    modelo.addAttribute("filasConAsientos", new ArrayList<>(mapFila.values()));
                }
            }
        }
        return "reservar";
    }

    @PostMapping("/reservar")
    public String procesar(@RequestParam String nombre,
                           @RequestParam String apellido,
                           @RequestParam String email,
                           @RequestParam String telefono,
                           @RequestParam Long peliculaId,
                           @RequestParam String fecha,
                           @RequestParam(defaultValue = "") String asientoIds,
                           RedirectAttributes ra) {

        List<Long> ids = parsearIds(asientoIds);
        if (ids.isEmpty()) {
            ra.addAttribute("peliculaId", peliculaId);
            ra.addAttribute("fecha", fecha);
            ra.addAttribute("errorAsientos", true);
            return "redirect:/reservar";
        }

        Cliente cliente = clienteRepo.findByEmail(email)
                .orElseGet(() -> clienteRepo.save(new Cliente(nombre + " " + apellido, email, telefono)));

        Pelicula pelicula = peliculaService.buscar(peliculaId);
        double total = pelicula.getPrecio() * ids.size();

        Long reservaId = asientoService.crearReserva(
                cliente.getId(), peliculaId, fecha, ids.size(), total, "Pendiente");
        asientoService.guardarAsientosReserva(reservaId, ids);

        ra.addAttribute("exito", true);
        return "redirect:/reservar";
    }

    private List<Long> parsearIds(String ids) {
        if (ids == null || ids.isBlank()) return new ArrayList<>();
        return Arrays.stream(ids.split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
