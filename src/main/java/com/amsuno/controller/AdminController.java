package com.amsuno.controller;

import com.amsuno.dto.ReservaDTO;
import com.amsuno.model.Cliente;
import com.amsuno.model.Pelicula;
import com.amsuno.model.Reserva;
import com.amsuno.model.Snack;
import com.amsuno.service.AsientoService;
import com.amsuno.service.ClienteService;
import com.amsuno.service.PeliculaService;
import com.amsuno.service.ReservaService;
import com.amsuno.service.SalaService;
import com.amsuno.service.SnackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PeliculaService peliculaService;
    private final ClienteService clienteService;
    private final ReservaService reservaService;
    private final SnackService snackService;
    private final AsientoService asientoService;
    private final SalaService salaService;

    public AdminController(PeliculaService peliculaService, ClienteService clienteService,
                           ReservaService reservaService, SnackService snackService,
                           AsientoService asientoService, SalaService salaService) {
        this.peliculaService = peliculaService;
        this.clienteService  = clienteService;
        this.reservaService  = reservaService;
        this.snackService    = snackService;
        this.asientoService  = asientoService;
        this.salaService     = salaService;
    }

    private boolean sinSesion(HttpSession sesion) {
        return sesion.getAttribute("loggedIn") == null;
    }

    private boolean sinPermiso(HttpSession sesion) {
        return !"ADMIN".equals(sesion.getAttribute("userRole"));
    }

    private ReservaDTO crearDTO(Reserva r) {
        return new ReservaDTO(r, asientoService.etiquetasAsientos(r.getId()));
    }

    private List<Long> parsearIds(String ids) {
        if (ids == null || ids.isBlank()) return new ArrayList<>();
        return Arrays.stream(ids.split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @GetMapping
    public String inicio(HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession sesion, Model modelo) {
        if (sinSesion(sesion)) return "redirect:/login";

        List<Reserva> reservas   = reservaService.listar();
        List<Pelicula> peliculas = peliculaService.listar();
        List<Cliente> clientes   = clienteService.listar();

        long totalIngresos = (long) reservas.stream().mapToDouble(Reserva::getTotal).sum();
        long pendientes    = reservas.stream().filter(r -> "Pendiente".equals(r.getEstado())).count();

        List<ReservaDTO> reservasDTO = reservas.stream().map(this::crearDTO).toList();

        modelo.addAttribute("reservas",      reservasDTO);
        modelo.addAttribute("peliculas",     peliculas);
        modelo.addAttribute("clientes",      clientes);
        modelo.addAttribute("totalIngresos", totalIngresos);
        modelo.addAttribute("pendientes",    pendientes);
        return "admin/dashboard";
    }

    @GetMapping("/reservas")
    public String reservas(@RequestParam(required = false) String buscar,
                           @RequestParam(required = false) String estado,
                           HttpSession sesion, Model modelo) {
        if (sinSesion(sesion)) return "redirect:/login";

        List<Reserva> lista = reservaService.listar();

        if (buscar != null && !buscar.isBlank()) {
            String t = buscar.toLowerCase();
            lista = lista.stream()
                    .filter(r -> r.getCliente().getNombre().toLowerCase().contains(t)
                              || r.getPelicula().getTitulo().toLowerCase().contains(t)
                              || r.getId().toString().contains(t))
                    .toList();
        }

        if (estado != null && !estado.isBlank() && !"Todos".equals(estado)) {
            lista = lista.stream().filter(r -> r.getEstado().equals(estado)).toList();
        }

        List<ReservaDTO> reservasDTO = lista.stream().map(this::crearDTO).toList();

        modelo.addAttribute("reservas",     reservasDTO);
        modelo.addAttribute("peliculas",    peliculaService.listar());
        modelo.addAttribute("clientes",     clienteService.listar());
        modelo.addAttribute("generos",      peliculaService.listarGeneros());
        modelo.addAttribute("buscar",       buscar != null ? buscar : "");
        modelo.addAttribute("estadoFiltro", estado  != null ? estado : "Todos");
        return "admin/reservas";
    }

    @GetMapping("/asientos")
    public String mapaAsientos(@RequestParam Long peliculaId,
                               @RequestParam String fecha,
                               @RequestParam Long clienteId,
                               @RequestParam(defaultValue = "Pendiente") String estado,
                               HttpSession sesion, Model modelo) {
        if (sinSesion(sesion)) return "redirect:/login";

        Pelicula pelicula = peliculaService.buscar(peliculaId);

        if (pelicula.getSala() == null) {
            return "redirect:/admin/reservas?error=sinSala";
        }

        List<Map<String, Object>> asientos = asientoService.asientosDisponibles(peliculaId, fecha);

        Map<String, List<Map<String, Object>>> mapFila = new LinkedHashMap<>();
        for (Map<String, Object> a : asientos) {
            mapFila.computeIfAbsent(a.get("fila").toString(), k -> new ArrayList<>()).add(a);
        }
        List<List<Map<String, Object>>> filasConAsientos = new ArrayList<>(mapFila.values());

        modelo.addAttribute("pelicula",          pelicula);
        modelo.addAttribute("cliente",           clienteService.buscar(clienteId));
        modelo.addAttribute("fecha",             fecha);
        modelo.addAttribute("estado",            estado);
        modelo.addAttribute("filasConAsientos",  filasConAsientos);
        return "admin/asientos";
    }

    @PostMapping("/reservas/nueva")
    public String nuevaReserva(@RequestParam Long clienteId,
                               @RequestParam Long peliculaId,
                               @RequestParam String fecha,
                               @RequestParam String estado,
                               @RequestParam(defaultValue = "") String asientoIds,
                               HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";

        List<Long> ids = parsearIds(asientoIds);
        if (ids.isEmpty()) return "redirect:/admin/reservas?error=sinAsientos";

        Pelicula pelicula = peliculaService.buscar(peliculaId);
        double total      = pelicula.getPrecio() * ids.size();

        Long reservaId = asientoService.crearReserva(clienteId, peliculaId, fecha, ids.size(), total, estado);
        asientoService.guardarAsientosReserva(reservaId, ids);

        return "redirect:/admin/reservas";
    }

    @GetMapping("/reservas/confirmar/{id}")
    public String confirmarReserva(@PathVariable Long id, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        reservaService.confirmar(id);
        return "redirect:/admin/reservas";
    }

    @GetMapping("/reservas/cancelar/{id}")
    public String cancelarReserva(@PathVariable Long id, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        asientoService.cancelarReserva(id);
        return "redirect:/admin/reservas";
    }

    @GetMapping("/reservas/eliminar/{id}")
    public String eliminarReserva(@PathVariable Long id, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        reservaService.eliminar(id);
        return "redirect:/admin/reservas";
    }

    @GetMapping("/peliculas")
    public String peliculas(HttpSession sesion, Model modelo) {
        if (sinSesion(sesion)) return "redirect:/login";
        modelo.addAttribute("peliculas", peliculaService.listar());
        modelo.addAttribute("pelicula",  new Pelicula());
        modelo.addAttribute("salas",     salaService.listar());
        return "admin/peliculas";
    }

    @PostMapping("/peliculas/agregar")
    public String agregarPelicula(@ModelAttribute Pelicula pelicula,
                                  @RequestParam(required = false) Long salaId,
                                  HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        if (salaId != null) pelicula.setSala(salaService.buscar(salaId));
        peliculaService.agregar(pelicula);
        return "redirect:/admin/peliculas";
    }

    @GetMapping("/peliculas/eliminar/{id}")
    public String eliminarPelicula(@PathVariable Long id, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        peliculaService.eliminar(id);
        return "redirect:/admin/peliculas";
    }

    @GetMapping("/clientes")
    public String clientes(HttpSession sesion, Model modelo) {
        if (sinSesion(sesion)) return "redirect:/login";

        List<Cliente> clientes = clienteService.listar();
        List<Reserva> reservas = reservaService.listar();

        Map<Long, Integer> reservasPorCliente = new HashMap<>();
        Map<Long, Integer> gastadoPorCliente  = new HashMap<>();

        for (Cliente c : clientes) {
            List<Reserva> misReservas = reservas.stream()
                    .filter(r -> r.getCliente().getId().equals(c.getId()))
                    .toList();
            reservasPorCliente.put(c.getId(), misReservas.size());
            gastadoPorCliente.put(c.getId(), (int) misReservas.stream().mapToDouble(Reserva::getTotal).sum());
        }

        List<ReservaDTO> reservasDTO = reservas.stream().map(this::crearDTO).toList();

        modelo.addAttribute("clientes",           clientes);
        modelo.addAttribute("cliente",             new Cliente());
        modelo.addAttribute("reservasPorCliente",  reservasPorCliente);
        modelo.addAttribute("gastadoPorCliente",   gastadoPorCliente);
        modelo.addAttribute("reservasDTO",         reservasDTO);
        return "admin/clientes";
    }

    @PostMapping("/clientes/agregar")
    public String agregarCliente(@ModelAttribute Cliente cliente, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        clienteService.agregar(cliente);
        return "redirect:/admin/clientes";
    }

    @GetMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable Long id, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        clienteService.eliminar(id);
        return "redirect:/admin/clientes";
    }

    @GetMapping("/snacks")
    public String snacks(HttpSession sesion, Model modelo) {
        if (sinSesion(sesion)) return "redirect:/login";
        modelo.addAttribute("snacks", snackService.listar());
        modelo.addAttribute("snack",  new Snack());
        return "admin/snacks";
    }

    @PostMapping("/snacks/agregar")
    public String agregarSnack(@ModelAttribute Snack snack, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        snackService.agregar(snack);
        return "redirect:/admin/snacks";
    }

    @GetMapping("/snacks/eliminar/{id}")
    public String eliminarSnack(@PathVariable Long id, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        snackService.eliminar(id);
        return "redirect:/admin/snacks";
    }

    @GetMapping("/estadisticas")
    public String estadisticas(HttpSession sesion, Model modelo) {
        if (sinSesion(sesion))  return "redirect:/login";
        if (sinPermiso(sesion)) return "redirect:/admin/dashboard?accesoDenegado=true";
        List<ReservaDTO> reservasDTO = reservaService.listar().stream().map(this::crearDTO).toList();
        modelo.addAttribute("totalClientes",  clienteService.listar().size());
        modelo.addAttribute("totalPeliculas", peliculaService.listar().size());
        modelo.addAttribute("totalReservas",  reservasDTO.size());
        modelo.addAttribute("peliculas",      peliculaService.listar());
        modelo.addAttribute("reservas",       reservasDTO);
        return "admin/estadisticas";
    }

    @GetMapping("/graficos")
    public String graficos(HttpSession sesion, Model modelo) {
        if (sinSesion(sesion))  return "redirect:/login";
        if (sinPermiso(sesion)) return "redirect:/admin/dashboard?accesoDenegado=true";
        List<ReservaDTO> reservasDTO = reservaService.listar().stream().map(this::crearDTO).toList();
        modelo.addAttribute("totalClientes",  clienteService.listar().size());
        modelo.addAttribute("totalPeliculas", peliculaService.listar().size());
        modelo.addAttribute("totalReservas",  reservasDTO.size());
        modelo.addAttribute("peliculas",      peliculaService.listar());
        modelo.addAttribute("reservas",       reservasDTO);
        return "admin/graficos";
    }

    @GetMapping("/configuracion")
    public String configuracion(HttpSession sesion) {
        if (sinSesion(sesion))  return "redirect:/login";
        if (sinPermiso(sesion)) return "redirect:/admin/dashboard?accesoDenegado=true";
        return "admin/configuracion";
    }

    @GetMapping("/cerrar-sesion")
    public String cerrarSesion(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/";
    }
}
