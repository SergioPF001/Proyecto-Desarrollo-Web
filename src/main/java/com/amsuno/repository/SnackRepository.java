package com.amsuno.repository;

import com.amsuno.model.Snack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnackRepository extends JpaRepository<Snack, Long> {}
