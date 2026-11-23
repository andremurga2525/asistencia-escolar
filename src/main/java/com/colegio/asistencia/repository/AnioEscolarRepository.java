package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.AnioEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnioEscolarRepository extends JpaRepository<AnioEscolar, Long> {

    Optional<AnioEscolar> findByAnio(Integer anio);

    Optional<AnioEscolar> findFirstByActivoTrue();
}
