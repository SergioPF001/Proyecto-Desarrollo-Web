package com.amsuno.controller.api;

import com.amsuno.dto.ReservaDTO;
import com.amsuno.exception.RecursoNoEncontradoException;
import com.amsuno.model.Reserva;
import com.amsuno.service.AsientoService;
import com.amsuno.service.ReservaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaApiController {

    private final ReservaService reservaService;
    private final AsientoService asientoService;

    public ReservaApiController(ReservaService reservaService, AsientoService asientoService) {
        this.reservaService = reservaService;
        this.asientoService = asientoService;
    }

    private Reserva buscarReserva(Long id) {
        Reserva reserva = reservaService.buscar(id);
        if (reserva == null) {
            throw new RecursoNoEncontradoException("No existe la reserva con id " + id);
        }
        return reserva;
    }

    private ReservaDTO crearDTO(Reserva reserva) {
        return new ReservaDTO(reserva, asientoService.etiquetasAsientos(reserva.getId()));
    }

    @GetMapping
    public List<ReservaDTO> listar(@RequestParam(required = false) String estado) {
        return reservaService.listar().stream()
                .filter(r -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(r.getEstado()))
                .map(this::crearDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ReservaDTO buscar(@PathVariable Long id) {
        return crearDTO(buscarReserva(id));
    }

    @PutMapping("/{id}/confirmar")
    public ReservaDTO confirmar(@PathVariable Long id) {
        buscarReserva(id);
        reservaService.confirmar(id);
        return crearDTO(buscarReserva(id));
    }

    @PutMapping("/{id}/cancelar")
    public ReservaDTO cancelar(@PathVariable Long id) {
        buscarReserva(id);
        asientoService.cancelarReserva(id);
        return crearDTO(buscarReserva(id));
    }
}
