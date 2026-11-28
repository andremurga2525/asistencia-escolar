package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.ReporteConducta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteConductaRepository extends JpaRepository<ReporteConducta, Long> {

    List<ReporteConducta> findByEstudianteIdOrderByFechaIncidenciaDesc(Long estudianteId);

    List<ReporteConducta> findByTutorIdOrderByFechaCreacionDesc(Long tutorId);

    List<ReporteConducta> findAllByOrderByFechaCreacionDesc();
}
