package com.amsuno.service;

import com.amsuno.model.Asiento;
import com.amsuno.model.Reserva;
import com.amsuno.model.ReservaAsiento;
import com.amsuno.repository.AsientoRepository;
import com.amsuno.repository.ReservaAsientoRepository;
import com.amsuno.repository.ReservaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Service
public class AsientoService {

    private final JdbcTemplate jdbc;
    private final AsientoRepository asientoRepo;
    private final ReservaAsientoRepository reservaAsientoRepo;
    private final ReservaRepository reservaRepo;

    public AsientoService(JdbcTemplate jdbc, AsientoRepository asientoRepo,
                          ReservaAsientoRepository reservaAsientoRepo,
                          ReservaRepository reservaRepo) {
        this.jdbc = jdbc;
        this.asientoRepo = asientoRepo;
        this.reservaAsientoRepo = reservaAsientoRepo;
        this.reservaRepo = reservaRepo;
    }

    public List<Map<String, Object>> asientosDisponibles(Long peliculaId, String fecha) {
        return jdbc.queryForList("CALL sp_AsientosDisponibles(?, ?)", peliculaId, fecha);
    }

    public Long crearReserva(Long clienteId, Long peliculaId, String fecha,
                             int cantidad, double total, String estado) {
        return jdbc.execute((java.sql.Connection con) -> {
            CallableStatement cs = con.prepareCall("{CALL sp_CrearReserva(?, ?, ?, ?, ?, ?, ?)}");
            cs.setLong(1, clienteId);
            cs.setLong(2, peliculaId);
            cs.setString(3, fecha);
            cs.setInt(4, cantidad);
            cs.setDouble(5, total);
            cs.setString(6, estado);
            cs.registerOutParameter(7, Types.BIGINT);
            cs.execute();
            return cs.getLong(7);
        });
    }

    @Transactional
    public void guardarAsientosReserva(Long reservaId, List<Long> asientoIds) {
        Reserva reserva = reservaRepo.findById(reservaId).orElseThrow();
        for (Long asientoId : asientoIds) {
            Asiento asiento = asientoRepo.findById(asientoId).orElseThrow();
            reservaAsientoRepo.save(new ReservaAsiento(reserva, asiento));
        }
    }

    public void cancelarReserva(Long reservaId) {
        jdbc.execute((java.sql.Connection con) -> {
            CallableStatement cs = con.prepareCall("{CALL sp_CancelarReserva(?)}");
            cs.setLong(1, reservaId);
            cs.execute();
            return null;
        });
    }

    public String etiquetasAsientos(Long reservaId) {
        List<String> etiquetas = reservaAsientoRepo.findEtiquetasByReservaId(reservaId);
        return etiquetas.isEmpty() ? "-" : String.join(", ", etiquetas);
    }
}
