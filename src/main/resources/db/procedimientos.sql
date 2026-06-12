DELIMITER //

DROP PROCEDURE IF EXISTS sp_AsientosDisponibles //
CREATE PROCEDURE sp_AsientosDisponibles(IN p_pelicula_id BIGINT, IN p_fecha VARCHAR(100))
BEGIN
    SELECT
        a.id,
        a.fila,
        a.numero,
        a.tipo,
        CASE
            WHEN EXISTS (
                SELECT 1 FROM reserva_asiento ra
                JOIN reserva r ON ra.reserva_id = r.id
                WHERE ra.asiento_id = a.id
                  AND r.pelicula_id = p_pelicula_id
                  AND r.fecha       = p_fecha
                  AND r.estado     != 'Cancelada'
            ) THEN 'ocupado'
            ELSE 'disponible'
        END AS estado
    FROM pelicula p
    JOIN sala    s ON p.sala_id  = s.id
    JOIN asiento a ON a.sala_id  = s.id
    WHERE p.id = p_pelicula_id
    ORDER BY a.fila, a.numero;
END //

DROP PROCEDURE IF EXISTS sp_CrearReserva //
CREATE PROCEDURE sp_CrearReserva(
    IN  p_cliente_id  BIGINT,
    IN  p_pelicula_id BIGINT,
    IN  p_fecha       VARCHAR(100),
    IN  p_cantidad    INT,
    IN  p_total       DOUBLE,
    IN  p_estado      VARCHAR(50),
    OUT p_reserva_id  BIGINT
)
BEGIN
    INSERT INTO reserva(cliente_id, pelicula_id, fecha, asientos, total, estado)
    VALUES (p_cliente_id, p_pelicula_id, p_fecha, p_cantidad, p_total, p_estado);
    SET p_reserva_id = LAST_INSERT_ID();
END //

DROP PROCEDURE IF EXISTS sp_CancelarReserva //
CREATE PROCEDURE sp_CancelarReserva(IN p_reserva_id BIGINT)
BEGIN
    DELETE FROM reserva_asiento WHERE reserva_id = p_reserva_id;
    UPDATE reserva SET estado = 'Cancelada' WHERE id = p_reserva_id;
END //

DROP PROCEDURE IF EXISTS sp_ResumenSala //
CREATE PROCEDURE sp_ResumenSala(IN p_sala_id BIGINT, IN p_fecha VARCHAR(100))
BEGIN
    SELECT
        COUNT(*) AS total,
        SUM(CASE WHEN EXISTS (
            SELECT 1 FROM reserva_asiento ra
            JOIN reserva r ON ra.reserva_id = r.id
            WHERE ra.asiento_id = a.id
              AND r.fecha        = p_fecha
              AND r.estado      != 'Cancelada'
        ) THEN 1 ELSE 0 END) AS ocupados,
        SUM(CASE WHEN NOT EXISTS (
            SELECT 1 FROM reserva_asiento ra
            JOIN reserva r ON ra.reserva_id = r.id
            WHERE ra.asiento_id = a.id
              AND r.fecha        = p_fecha
              AND r.estado      != 'Cancelada'
        ) THEN 1 ELSE 0 END) AS disponibles
    FROM asiento a
    WHERE a.sala_id = p_sala_id;
END //

DELIMITER ;
