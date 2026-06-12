package com.amsuno.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reserva_asiento")
public class ReservaAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "asiento_id")
    private Asiento asiento;

    public ReservaAsiento() {}

    public ReservaAsiento(Reserva reserva, Asiento asiento) {
        this.reserva = reserva;
        this.asiento = asiento;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }
}
