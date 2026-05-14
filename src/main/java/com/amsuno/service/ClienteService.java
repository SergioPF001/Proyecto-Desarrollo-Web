package com.amsuno.service;

import com.amsuno.model.Cliente;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ClienteService {

    private final List<Cliente> clientes = new ArrayList<>();
    private final AtomicLong contador = new AtomicLong();

    public ClienteService() {
        clientes.add(new Cliente(contador.incrementAndGet(), "Carlos Mendoza", "carlos@email.com", "08012345678"));
        clientes.add(new Cliente(contador.incrementAndGet(), "María García",   "maria@email.com",  "08098765432"));
        clientes.add(new Cliente(contador.incrementAndGet(), "José Rodríguez", "jose@email.com",   "08056781234"));
    }

    public List<Cliente> listar() {
        return clientes;
    }

    public void agregar(Cliente cliente) {
        cliente.setId(contador.incrementAndGet());
        clientes.add(cliente);
    }

    public Cliente buscar(Long id) {
        return clientes.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void eliminar(Long id) {
        clientes.removeIf(c -> c.getId().equals(id));
    }
}
