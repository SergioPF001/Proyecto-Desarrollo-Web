package com.amsuno.service;

import com.amsuno.dto.DniDTO;
import com.amsuno.dto.TipoCambioDTO;
import com.amsuno.exception.ConsultaExternaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ConsultaExternaService {

    private final RestTemplate restTemplate;
    private final String urlTipoCambio;
    private final String urlDni;
    private final String tokenDni;

    public ConsultaExternaService(RestTemplate restTemplate,
                                  @Value("${api.tipo-cambio.url}") String urlTipoCambio,
                                  @Value("${api.dni.url}") String urlDni,
                                  @Value("${api.dni.token}") String tokenDni) {
        this.restTemplate  = restTemplate;
        this.urlTipoCambio = urlTipoCambio;
        this.urlDni        = urlDni;
        this.tokenDni      = tokenDni;
    }

    @SuppressWarnings("unchecked")
    public TipoCambioDTO tipoCambio() {
        Map<String, Object> respuesta;

        try {
            respuesta = restTemplate.getForObject(urlTipoCambio, Map.class);
        } catch (RestClientException e) {
            throw new ConsultaExternaException("No se pudo conectar con el servicio de tipo de cambio.");
        }

        if (respuesta == null) {
            throw new ConsultaExternaException("El servicio de tipo de cambio no respondió.");
        }

        Map<String, Object> tasas = (Map<String, Object>) respuesta.get("rates");
        if (tasas == null || tasas.get("PEN") == null) {
            throw new ConsultaExternaException("El servicio de tipo de cambio no incluye la moneda PEN.");
        }

        double valor = ((Number) tasas.get("PEN")).doubleValue();
        Object fecha = respuesta.get("time_last_update_utc");

        return new TipoCambioDTO("USD", "PEN", valor, fecha != null ? fecha.toString() : "-");
    }

    public DniDTO consultarDni(String numero) {
        if (numero == null || !numero.matches("\\d{8}")) {
            throw new ConsultaExternaException("El DNI debe tener exactamente 8 dígitos.");
        }
        if (tokenDni == null || tokenDni.isBlank()) {
            throw new ConsultaExternaException("Falta configurar 'api.dni.token' en application.properties.");
        }

        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setBearerAuth(tokenDni);
        HttpEntity<Void> peticion = new HttpEntity<>(cabeceras);

        DniDTO persona;
        try {
            persona = restTemplate.exchange(urlDni + "?numero=" + numero,
                    HttpMethod.GET, peticion, DniDTO.class).getBody();
        } catch (RestClientException e) {
            throw new ConsultaExternaException(
                    "No se pudo consultar el DNI " + numero + ". Verifica el token y tu conexión.");
        }

        if (persona == null || persona.getNombres() == null) {
            throw new ConsultaExternaException("No se encontraron datos para el DNI " + numero + ".");
        }
        return persona;
    }
}
