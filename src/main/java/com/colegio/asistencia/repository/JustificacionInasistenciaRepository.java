package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.EnumEstadoJustificacion;
import com.colegio.asistencia.model.JustificacionInasistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JustificacionInasistenciaRepository extends JpaRepository<JustificacionInasistencia, Long> {

    Optional<JustificacionInasistencia> findByRegistroAsistenciaId(Long registroAsistenciaId);

    List<JustificacionInasistencia> findByApoderadoIdOrderByFechaSolicitudDesc(Long apoderadoId);

    List<JustificacionInasistencia> findAllByOrderByFechaSolicitudDesc();

    List<JustificacionInasistencia> findByEstado(EnumEstadoJustificacion estado);

    List<JustificacionInasistencia> findByEstadoOrderByFechaSolicitudDesc(EnumEstadoJustificacion estado);

    long countByEstado(EnumEstadoJustificacion estado);
}
