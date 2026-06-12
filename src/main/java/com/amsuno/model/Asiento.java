package com.amsuno.model;

import jakarta.persistence.*;

@Entity
@Table(name = "asiento")
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sala_id")
    private Sala sala;

    private String fila;
    private int numero;
    private String tipo;

    public Asiento() {}

    public Asiento(Sala sala, String fila, int numero, String tipo) {
        this.sala = sala;
        this.fila = fila;
        this.numero = numero;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sala getSala() { return sala; }
    public void setSala(Sala sala) { this.sala = sala; }
    public String getFila() { return fila; }
    public void setFila(String fila) { this.fila = fila; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
