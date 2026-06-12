package com.amsuno.repository;

import com.amsuno.model.ReservaAsiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReservaAsientoRepository extends JpaRepository<ReservaAsiento, Long> {

    List<ReservaAsiento> findByReservaId(Long reservaId);

    void deleteByReservaId(Long reservaId);

    @Query("SELECT CONCAT(ra.asiento.fila, ra.asiento.numero) FROM ReservaAsiento ra WHERE ra.reserva.id = :reservaId ORDER BY ra.asiento.fila, ra.asiento.numero")
    List<String> findEtiquetasByReservaId(@Param("reservaId") Long reservaId);
}
