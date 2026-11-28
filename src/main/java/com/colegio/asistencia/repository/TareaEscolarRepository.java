package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.TareaEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaEscolarRepository extends JpaRepository<TareaEscolar, Long> {

    List<TareaEscolar> findByGradoIdAndSeccionIdAndActivoTrueOrderByFechaEntregaAsc(Long gradoId, Long seccionId);

    List<TareaEscolar> findByGradoIdAndActivoTrueOrderByFechaEntregaAsc(Long gradoId);

    List<TareaEscolar> findByProfesorIdOrderByFechaCreacionDesc(Long profesorId);

    List<TareaEscolar> findAllByOrderByFechaCreacionDesc();
}
