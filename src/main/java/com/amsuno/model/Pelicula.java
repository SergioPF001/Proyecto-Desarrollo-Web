package com.amsuno.model;

import jakarta.persistence.*;

@Entity
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String genero;
    private int duracion;
    private String clasificacion;
    private double precio;
    private boolean esEstreno;
    private String horarios;

    @ManyToOne
    @JoinColumn(name = "sala_id")
    private Sala sala;

    public Pelicula() {}

    public Pelicula(String titulo, String genero, int duracion, String clasificacion,
                    double precio, boolean esEstreno, String horarios) {
        this.titulo = titulo;
        this.genero = genero;
        this.duracion = duracion;
        this.clasificacion = clasificacion;
        this.precio = precio;
        this.esEstreno = esEstreno;
        this.horarios = horarios;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public int getDuracion() { return duracion; }
    public void setDuracion(int duracion) { this.duracion = duracion; }
    public String getClasificacion() { return clasificacion; }
    public void setClasificacion(String clasificacion) { this.clasificacion = clasificacion; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public boolean isEsEstreno() { return esEstreno; }
    public void setEsEstreno(boolean esEstreno) { this.esEstreno = esEstreno; }
    public String getHorarios() { return horarios; }
    public void setHorarios(String horarios) { this.horarios = horarios; }
    public Sala getSala() { return sala; }
    public void setSala(Sala sala) { this.sala = sala; }
}
