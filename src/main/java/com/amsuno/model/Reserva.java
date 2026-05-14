package com.amsuno.model;

public class Reserva {
    private Long id;
    private String clienteNombre;
    private String pelicula;
    private String fecha;
    private int asientos;
    private double total;
    private String estado;

    public Reserva() {}

    public Reserva(Long id, String clienteNombre, String pelicula, String fecha, int asientos, double total, String estado) {
        this.id = id;
        this.clienteNombre = clienteNombre;
        this.pelicula = pelicula;
        this.fecha = fecha;
        this.asientos = asientos;
        this.total = total;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getPelicula() { return pelicula; }
    public void setPelicula(String pelicula) { this.pelicula = pelicula; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public int getAsientos() { return asientos; }
    public void setAsientos(int asientos) { this.asientos = asientos; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
