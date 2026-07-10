package com.amsuno.controller.api;

import com.amsuno.dto.DniDTO;
import com.amsuno.dto.TipoCambioDTO;
import com.amsuno.service.ConsultaExternaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaApiController {

    private final ConsultaExternaService consultaExternaService;

    public ConsultaApiController(ConsultaExternaService consultaExternaService) {
        this.consultaExternaService = consultaExternaService;
    }

    @GetMapping("/tipo-cambio")
    public TipoCambioDTO tipoCambio() {
        return consultaExternaService.tipoCambio();
    }

    @GetMapping("/dni/{numero}")
    public DniDTO dni(@PathVariable String numero) {
        return consultaExternaService.consultarDni(numero);
    }
}
