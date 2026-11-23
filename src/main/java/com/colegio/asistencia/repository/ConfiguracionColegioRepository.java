package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.ConfiguracionColegio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionColegioRepository extends JpaRepository<ConfiguracionColegio, Long> {

    Optional<ConfiguracionColegio> findFirstByActivoTrue();
}
