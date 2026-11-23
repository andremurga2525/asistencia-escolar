package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.EnumTurno;
import com.colegio.asistencia.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    Optional<Turno> findByCodigo(EnumTurno codigo);
}
