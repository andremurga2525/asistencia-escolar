package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.EstadoPuntualidad;
import com.colegio.asistencia.model.RegistroAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    Optional<RegistroAsistencia> findByEstudianteIdAndFecha(Long estudianteId, LocalDate fecha);

    Optional<RegistroAsistencia> findByEstudianteCodigoQrAndFecha(String codigoQr, LocalDate fecha);

    List<RegistroAsistencia> findByFechaOrderByHoraEntradaAsc(LocalDate fecha);

    List<RegistroAsistencia> findByEstudianteDniOrderByFechaDesc(String dni);

    long countByFecha(LocalDate fecha);

    long countByFechaAndEstado(LocalDate fecha, EstadoPuntualidad estado);

    @Query("SELECT r.estado, COUNT(r) FROM RegistroAsistencia r WHERE r.fecha = :fecha GROUP BY r.estado")
    List<Object[]> countResumenPorEstadoYFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT r FROM RegistroAsistencia r WHERE " +
           "(:desde IS NULL OR r.fecha >= :desde) AND " +
           "(:hasta IS NULL OR r.fecha <= :hasta) AND " +
           "(:estado IS NULL OR r.estado = :estado) " +
           "ORDER BY r.fecha DESC, r.horaEntrada ASC")
    List<RegistroAsistencia> filtrarReportes(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estado") EstadoPuntualidad estado);
}
