package com.amsuno.dto;

public class TipoCambioDTO {

    private String monedaBase;
    private String monedaDestino;
    private double valor;
    private String actualizado;

    public TipoCambioDTO(String monedaBase, String monedaDestino, double valor, String actualizado) {
        this.monedaBase = monedaBase;
        this.monedaDestino = monedaDestino;
        this.valor = valor;
        this.actualizado = actualizado;
    }

    public String getMonedaBase() { return monedaBase; }
    public String getMonedaDestino() { return monedaDestino; }
    public double getValor() { return valor; }
    public String getActualizado() { return actualizado; }
}
