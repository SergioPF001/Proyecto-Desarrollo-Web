package com.amsuno.repository;

import com.amsuno.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByEstado(String estado);
    List<Reserva> findByCliente_Id(Long clienteId);
}
