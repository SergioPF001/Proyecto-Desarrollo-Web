package com.amsuno.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "pelicula_id")
    private Pelicula pelicula;

    private String fecha;
    private int asientos;
    private double total;
    private String estado;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.REMOVE)
    private List<ReservaAsiento> asientosReservados = new ArrayList<>();

    public Reserva() {}

    public Reserva(Cliente cliente, Pelicula pelicula, String fecha,
                   int asientos, double total, String estado) {
        this.cliente = cliente;
        this.pelicula = pelicula;
        this.fecha = fecha;
        this.asientos = asientos;
        this.total = total;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Pelicula getPelicula() { return pelicula; }
    public void setPelicula(Pelicula pelicula) { this.pelicula = pelicula; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public int getAsientos() { return asientos; }
    public void setAsientos(int asientos) { this.asientos = asientos; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<ReservaAsiento> getAsientosReservados() { return asientosReservados; }
}
