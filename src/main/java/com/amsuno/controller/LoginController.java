package com.amsuno.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private static final String USUARIO_ADMIN   = "admin_cine";
    private static final String CONTRASENA_ADMIN = "LaEstacion2026!";

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
        if (USUARIO_ADMIN.equals(usuario) && CONTRASENA_ADMIN.equals(contrasena)) {
            sesion.setAttribute("loggedIn", true);
            return "redirect:/admin/dashboard";
        }
        modelo.addAttribute("error", "Usuario o contraseña incorrectos.");
        return "login";
    }
}
