package com.amsuno.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class LoginController {

    private static final Map<String, String[]> USUARIOS = Map.of(
        "admin_cine",  new String[]{"LaEstacion2026!", "ADMIN",  "Administrador"},
        "cajero_cine", new String[]{"Cajero2026!",     "CAJERO", "Cajero"}
    );

    @GetMapping("/login")
    public String mostrarLogin(HttpSession sesion) {
        if (sesion.getAttribute("loggedIn") != null) {
            return "redirect:/admin/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String usuario,
                                @RequestParam String contrasena,
                                HttpSession sesion,
                                Model modelo) {
        String[] datos = USUARIOS.get(usuario);
        if (datos != null && datos[0].equals(contrasena)) {
            sesion.setAttribute("loggedIn",   true);
            sesion.setAttribute("userRole",   datos[1]);
            sesion.setAttribute("userName",   usuario);
            sesion.setAttribute("userNombre", datos[2]);
            return "redirect:/admin/dashboard";
        }
        modelo.addAttribute("error", "Usuario o contraseña incorrectos.");
        return "login";
    }
}
