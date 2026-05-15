package com.amsuno.controller;

import com.amsuno.model.Cliente;
import com.amsuno.model.Pelicula;
import com.amsuno.model.Reserva;
import com.amsuno.model.Snack;
import com.amsuno.service.ClienteService;
import com.amsuno.service.PeliculaService;
import com.amsuno.service.ReservaService;
import com.amsuno.service.SnackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PeliculaService peliculaService;
    private final ClienteService clienteService;
    private final ReservaService reservaService;
    private final SnackService snackService;

    public AdminController(PeliculaService peliculaService, ClienteService clienteService,
                           ReservaService reservaService, SnackService snackService) {
        this.peliculaService = peliculaService;
        this.clienteService  = clienteService;
        this.reservaService  = reservaService;
        this.snackService    = snackService;
    }

    private boolean sinSesion(HttpSession sesion) {
        return sesion.getAttribute("loggedIn") == null;
    }

    private boolean sinPermiso(HttpSession sesion) {
        return !"ADMIN".equals(sesion.getAttribute("userRole"));
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

        modelo.addAttribute("reservas",       reservas);
        modelo.addAttribute("peliculas",      peliculas);
        modelo.addAttribute("clientes",       clientes);
        modelo.addAttribute("totalIngresos",  totalIngresos);
        modelo.addAttribute("pendientes",     pendientes);
        return "admin/dashboard";
    }

    @GetMapping("/reservas")
    public String reservas(@RequestParam(required = false) String buscar,
                           @RequestParam(required = false) String estado,
                           HttpSession sesion, Model modelo) {
        if (sinSesion(sesion)) return "redirect:/login";

        List<Reserva> lista = reservaService.listar();

        if (buscar != null && !buscar.isBlank()) {
            String termino = buscar.toLowerCase();
            lista = lista.stream()
                    .filter(r -> r.getClienteNombre().toLowerCase().contains(termino)
                              || r.getPelicula().toLowerCase().contains(termino)
                              || r.getId().toString().contains(termino))
                    .toList();
        }

        if (estado != null && !estado.isBlank() && !"Todos".equals(estado)) {
            lista = lista.stream()
                    .filter(r -> r.getEstado().equals(estado))
                    .toList();
        }

        modelo.addAttribute("reservas",     lista);
        modelo.addAttribute("reserva",      new Reserva());
        modelo.addAttribute("peliculas",    peliculaService.listar());
        modelo.addAttribute("buscar",       buscar != null ? buscar : "");
        modelo.addAttribute("estadoFiltro", estado  != null ? estado : "Todos");
        return "admin/reservas";
    }

    @PostMapping("/reservas/nueva")
    public String nuevaReserva(@ModelAttribute Reserva reserva, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        reservaService.agregar(reserva);
        return "redirect:/admin/reservas";
    }

    @GetMapping("/reservas/confirmar/{id}")
    public String confirmarReserva(@PathVariable Long id, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
        reservaService.confirmar(id);
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
        return "admin/peliculas";
    }

    @PostMapping("/peliculas/agregar")
    public String agregarPelicula(@ModelAttribute Pelicula pelicula, HttpSession sesion) {
        if (sinSesion(sesion)) return "redirect:/login";
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

        List<Cliente> clientes   = clienteService.listar();
        List<Reserva> reservas   = reservaService.listar();

        Map<String, Integer> pedidosPorCliente = new HashMap<>();
        Map<String, Integer> gastadoPorCliente = new HashMap<>();

        for (Cliente c : clientes) {
            List<Reserva> misReservas = reservas.stream()
                    .filter(r -> r.getClienteNombre().equals(c.getNombre()))
                    .toList();

            pedidosPorCliente.put(c.getNombre(), misReservas.size());
            gastadoPorCliente.put(c.getNombre(), (int) misReservas.stream().mapToDouble(Reserva::getTotal).sum());
        }

        modelo.addAttribute("clientes",          clientes);
        modelo.addAttribute("cliente",            new Cliente());
        modelo.addAttribute("pedidosPorCliente",  pedidosPorCliente);
        modelo.addAttribute("gastadoPorCliente",  gastadoPorCliente);
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
        modelo.addAttribute("totalClientes",  clienteService.listar().size());
        modelo.addAttribute("totalPeliculas", peliculaService.listar().size());
        modelo.addAttribute("totalReservas",  reservaService.listar().size());
        modelo.addAttribute("peliculas",      peliculaService.listar());
        modelo.addAttribute("reservas",       reservaService.listar());
        return "admin/estadisticas";
    }

    @GetMapping("/graficos")
    public String graficos(HttpSession sesion, Model modelo) {
        if (sinSesion(sesion))  return "redirect:/login";
        if (sinPermiso(sesion)) return "redirect:/admin/dashboard?accesoDenegado=true";
        modelo.addAttribute("totalClientes",  clienteService.listar().size());
        modelo.addAttribute("totalPeliculas", peliculaService.listar().size());
        modelo.addAttribute("totalReservas",  reservaService.listar().size());
        modelo.addAttribute("peliculas",      peliculaService.listar());
        modelo.addAttribute("clientes",       clienteService.listar());
        modelo.addAttribute("reservas",       reservaService.listar());
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
