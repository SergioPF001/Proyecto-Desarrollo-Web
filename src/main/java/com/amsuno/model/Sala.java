package com.amsuno.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sala")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private int filas;
    private int columnas;

    public Sala() {}

    public Sala(String nombre, int filas, int columnas) {
        this.nombre = nombre;
        this.filas = filas;
        this.columnas = columnas;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getFilas() { return filas; }
    public void setFilas(int filas) { this.filas = filas; }
    public int getColumnas() { return columnas; }
    public void setColumnas(int columnas) { this.columnas = columnas; }
}
