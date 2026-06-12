package com.amsuno.dto;

import com.amsuno.model.Reserva;

public class ReservaDTO {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private String clienteEmail;
    private String peliculaTitulo;
    private String peliculaGenero;
    private String fecha;
    private int asientos;
    private double total;
    private String estado;
    private String asientosDetalle;

    public ReservaDTO(Reserva r) {
        this(r, "-");
    }

    public ReservaDTO(Reserva r, String asientosDetalle) {
        this.id = r.getId();
        this.clienteId = r.getCliente().getId();
        this.clienteNombre = r.getCliente().getNombre();
        this.clienteEmail = r.getCliente().getEmail();
        this.peliculaTitulo = r.getPelicula().getTitulo();
        this.peliculaGenero = r.getPelicula().getGenero();
        this.fecha = r.getFecha();
        this.asientos = r.getAsientos();
        this.total = r.getTotal();
        this.estado = r.getEstado();
        this.asientosDetalle = asientosDetalle;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteEmail() { return clienteEmail; }
    public String getPeliculaTitulo() { return peliculaTitulo; }
    public String getPeliculaGenero() { return peliculaGenero; }
    public String getFecha() { return fecha; }
    public int getAsientos() { return asientos; }
    public double getTotal() { return total; }
    public String getEstado() { return estado; }
    public String getAsientosDetalle() { return asientosDetalle; }
}
