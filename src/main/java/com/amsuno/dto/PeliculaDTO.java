package com.amsuno.dto;

import com.amsuno.model.Pelicula;

import java.util.Arrays;
import java.util.List;

public class PeliculaDTO {

    private Long id;
    private String titulo;
    private String genero;
    private int duracion;
    private String clasificacion;
    private double precio;
    private boolean esEstreno;
    private List<String> horarios;
    private String sala;

    public PeliculaDTO(Pelicula p) {
        this.id = p.getId();
        this.titulo = p.getTitulo();
        this.genero = p.getGenero();
        this.duracion = p.getDuracion();
        this.clasificacion = p.getClasificacion();
        this.precio = p.getPrecio();
        this.esEstreno = p.isEsEstreno();
        this.horarios = separarHorarios(p.getHorarios());
        this.sala = p.getSala() != null ? p.getSala().getNombre() : "Sin sala asignada";
    }

    private List<String> separarHorarios(String horarios) {
        if (horarios == null || horarios.isBlank()) return List.of();
        return Arrays.stream(horarios.split(",")).map(String::trim).toList();
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getGenero() { return genero; }
    public int getDuracion() { return duracion; }
    public String getClasificacion() { return clasificacion; }
    public double getPrecio() { return precio; }
    public boolean isEsEstreno() { return esEstreno; }
    public List<String> getHorarios() { return horarios; }
    public String getSala() { return sala; }
}
