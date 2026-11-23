package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.EnumNivel;
import com.colegio.asistencia.model.EnumTurno;
import com.colegio.asistencia.model.HorarioEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioEscolarRepository extends JpaRepository<HorarioEscolar, Long> {

    List<HorarioEscolar> findByActivoTrue();

    Optional<HorarioEscolar> findByNivelCodigoAndTurnoCodigoAndActivoTrue(EnumNivel nivel, EnumTurno turno);

    List<HorarioEscolar> findAllByOrderByNivelIdAsc();
}
