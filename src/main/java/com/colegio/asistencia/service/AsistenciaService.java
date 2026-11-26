package com.colegio.asistencia.service;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class AsistenciaService {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaService.class);

    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final HorarioEscolarRepository horarioRepository;
    private final RegistroAsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;
    private final EmailNotificacionService emailService;

    public AsistenciaService(
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            HorarioEscolarRepository horarioRepository,
            RegistroAsistenciaRepository asistenciaRepository,
            UsuarioRepository usuarioRepository,
            JustificacionInasistenciaRepository justificacionRepository,
            @Autowired(required = false) EmailNotificacionService emailService) {
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.horarioRepository = horarioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.justificacionRepository = justificacionRepository;
        this.emailService = emailService;
    }

    @Transactional
    public RegistroAsistencia registrarEntradaPorQr(String codigoQr, String usernameAuxiliar) {
        Estudiante estudiante = estudianteRepository.findByCodigoQr(codigoQr.trim())
                .orElseThrow(() -> new IllegalArgumentException("Código QR inválido o no reconocido."));

        if (!Boolean.TRUE.equals(estudiante.getActivo())) {
            throw new IllegalStateException("El estudiante se encuentra inactivo.");
        }

        Matricula matricula = matriculaRepository.findByEstudianteCodigoQrAndAnioEscolarActivoTrue(codigoQr.trim())
                .orElseThrow(() -> new IllegalStateException("El estudiante no cuenta con matrícula activa para el año lectivo."));

        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        // Validar si ya registró asistencia hoy
        Optional<RegistroAsistencia> existenteOpt = asistenciaRepository.findByEstudianteIdAndFecha(estudiante.getId(), hoy);
        if (existenteOpt.isPresent()) {
            throw new IllegalStateException("El estudiante " + estudiante.getNombreCompleto() + " ya cuenta con registro de entrada para el día de hoy.");
        }

        // Buscar horario correspondiente al nivel y turno del estudiante
        EnumNivel nivel = matricula.getSeccion().getGrado().getNivel().getCodigo();
        EnumTurno turno = matricula.getTurno().getCodigo();

        HorarioEscolar horario = horarioRepository.findByNivelCodigoAndTurnoCodigoAndActivoTrue(nivel, turno)
                .orElseThrow(() -> new IllegalStateException("No se ha configurado un horario escolar activo para " + nivel.getDescripcion() + " - " + turno.getDescripcion()));

        // Evaluar puntualidad con la regla oficial del servidor
        EstadoPuntualidad estado = horario.evaluarEntrada(ahora);
        int minutosTardanza = 0;
        if (estado == EstadoPuntualidad.TARDANZA) {
            LocalTime limitePuntualidad = horario.getLimitePuntualidad();
            minutosTardanza = (int) java.time.Duration.between(limitePuntualidad, ahora).toMinutes();
            if (minutosTardanza < 0) minutosTardanza = 0;
        }

        Usuario auxiliar = usuarioRepository.findByUsername(usernameAuxiliar).orElse(null);

        RegistroAsistencia registro = new RegistroAsistencia(
                estudiante,
                matricula.getAnioEscolar(),
                matricula,
                hoy,
                ahora,
                estado,
                auxiliar
        );
        registro.setMinutosTardanza(minutosTardanza);
        registro.setObservaciones("Entrada registrada mediante escaneo QR por auxiliar: " + usernameAuxiliar);

        RegistroAsistencia guardado = asistenciaRepository.save(registro);
        if (emailService != null) {
            try {
                emailService.notificarEntrada(guardado);
            } catch (Exception e) {
                log.error("Error al notificar entrada por correo: {}", e.getMessage());
            }
        }
        return guardado;
    }

    @Transactional
    public RegistroAsistencia registrarSalidaPorQr(String codigoQr, String usernameAuxiliar) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        RegistroAsistencia registro = asistenciaRepository.findByEstudianteCodigoQrAndFecha(codigoQr.trim(), hoy)
                .orElseThrow(() -> new IllegalStateException("No existe un registro de entrada para este estudiante el día de hoy. Debe registrar su entrada primero."));

        if (registro.getHoraSalida() != null) {
            throw new IllegalStateException("El estudiante ya registró su salida a las " + registro.getHoraSalida());
        }

        registro.setHoraSalida(ahora);
        RegistroAsistencia guardado = asistenciaRepository.save(registro);
        if (emailService != null) {
            try {
                emailService.notificarSalida(guardado);
            } catch (Exception e) {
                log.error("Error al notificar salida por correo: {}", e.getMessage());
            }
        }
        return guardado;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasDashboard(LocalDate fecha) {
        Map<String, Object> stats = new HashMap<>();

        long totalEstudiantes = estudianteRepository.count();
        long totalRegistrosHoy = asistenciaRepository.countByFecha(fecha);
        long puntuales = asistenciaRepository.countByFechaAndEstado(fecha, EstadoPuntualidad.PUNTUAL);
        long anticipados = asistenciaRepository.countByFechaAndEstado(fecha, EstadoPuntualidad.TEMPRANO);
        long tardanzas = asistenciaRepository.countByFechaAndEstado(fecha, EstadoPuntualidad.TARDANZA);
        long inasistencias = asistenciaRepository.countByFechaAndEstado(fecha, EstadoPuntualidad.INASISTENCIA);
        long justificados = asistenciaRepository.countByFechaAndEstado(fecha, EstadoPuntualidad.JUSTIFICADO);
        long justificacionesPendientes = justificacionRepository.countByEstado(EnumEstadoJustificacion.PENDIENTE);

        double porcentajePuntualidad = totalRegistrosHoy > 0
                ? Math.round(((double) (puntuales + anticipados) / totalRegistrosHoy) * 1000.0) / 10.0
                : 0.0;

        stats.put("totalEstudiantes", totalEstudiantes);
        stats.put("totalRegistrosHoy", totalRegistrosHoy);
        stats.put("puntuales", puntuales + anticipados);
        stats.put("tardanzas", tardanzas);
        stats.put("inasistencias", inasistencias);
        stats.put("justificados", justificados);
        stats.put("porcentajePuntualidad", porcentajePuntualidad);
        stats.put("justificacionesPendientes", justificacionesPendientes);

        return stats;
    }
}
