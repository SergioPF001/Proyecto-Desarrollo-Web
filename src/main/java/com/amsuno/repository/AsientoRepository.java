package com.amsuno.repository;

import com.amsuno.model.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {
    List<Asiento> findBySalaId(Long salaId);
}
