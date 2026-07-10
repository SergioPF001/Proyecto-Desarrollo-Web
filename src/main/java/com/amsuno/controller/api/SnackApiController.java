package com.amsuno.controller.api;

import com.amsuno.model.Snack;
import com.amsuno.service.SnackService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/snacks")
public class SnackApiController {

    private final SnackService snackService;

    public SnackApiController(SnackService snackService) {
        this.snackService = snackService;
    }

    @GetMapping
    public List<Snack> listar() {
        return snackService.listar();
    }

    @GetMapping("/{id}")
    public Snack buscar(@PathVariable Long id) {
        return snackService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Snack crear(@RequestBody Snack snack) {
        return snackService.agregar(snack);
    }

    @PutMapping("/{id}")
    public Snack actualizar(@PathVariable Long id, @RequestBody Snack snack) {
        return snackService.actualizar(id, snack);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        snackService.eliminar(id);
    }
}
