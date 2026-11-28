package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.ComunicadoAula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComunicadoAulaRepository extends JpaRepository<ComunicadoAula, Long> {

    List<ComunicadoAula> findAllByOrderByFechaPublicacionDesc();

    List<ComunicadoAula> findByGradoIdAndSeccionIdOrderByFechaPublicacionDesc(Long gradoId, Long seccionId);

    List<ComunicadoAula> findByTutorIdOrderByFechaPublicacionDesc(Long tutorId);
}
