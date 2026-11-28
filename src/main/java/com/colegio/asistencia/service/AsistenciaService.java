package com.colegio.asistencia.service;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import com.colegio.asistencia.util.FechaUtil;
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

    private String limpiarCodigo(String raw) {
        if (raw == null) return "";
        String c = raw.trim();
        if (c.contains("/")) {
            c = c.substring(c.lastIndexOf('/') + 1);
        }
        if (c.contains("=")) {
            c = c.substring(c.lastIndexOf('=') + 1);
        }
        return c.trim();
    }

    @Transactional
    public RegistroAsistencia registrarEntradaPorQr(String codigoQr, String usernameAuxiliar) {
        String codigoLimpio = limpiarCodigo(codigoQr);
        Estudiante estudiante = estudianteRepository.findByCodigoQr(codigoLimpio)
                .or(() -> estudianteRepository.findByDni(codigoLimpio))
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún estudiante con el código QR o DNI: " + codigoLimpio));

        if (!Boolean.TRUE.equals(estudiante.getActivo())) {
            throw new IllegalStateException("El estudiante " + estudiante.getNombreCompleto() + " se encuentra inactivo en el sistema.");
        }

        Matricula matricula = matriculaRepository.findByEstudianteIdAndAnioEscolarActivoTrue(estudiante.getId())
                .orElseThrow(() -> new IllegalStateException("El estudiante " + estudiante.getNombreCompleto() + " no cuenta con matrícula activa para el año lectivo."));

        LocalDate hoy = FechaUtil.hoyPeru();
        LocalTime ahora = FechaUtil.ahoraHoraPeru();

        // Validar si ya registró asistencia hoy
        Optional<RegistroAsistencia> existenteOpt = asistenciaRepository.findByEstudianteIdAndFecha(estudiante.getId(), hoy);
        if (existenteOpt.isPresent()) {
            RegistroAsistencia yaRegistrado = existenteOpt.get();
            String horaIngreso = yaRegistrado.getHoraEntrada() != null 
                    ? yaRegistrado.getHoraEntrada().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                    : "hora registrada";
            throw new IllegalStateException(estudiante.getNombreCompleto() + " ya ingresó hoy a las " + horaIngreso + ". (Para marcar salida, use la opción 'REGISTRAR SALIDA').");
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
        String codigoLimpio = limpiarCodigo(codigoQr);
        Estudiante estudiante = estudianteRepository.findByCodigoQr(codigoLimpio)
                .or(() -> estudianteRepository.findByDni(codigoLimpio))
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún estudiante con el código QR o DNI: " + codigoLimpio));

        LocalDate hoy = FechaUtil.hoyPeru();
        LocalTime ahora = FechaUtil.ahoraHoraPeru();

        RegistroAsistencia registro = asistenciaRepository.findByEstudianteIdAndFecha(estudiante.getId(), hoy)
                .orElseThrow(() -> new IllegalStateException("No existe un registro de entrada para " + estudiante.getNombreCompleto() + " el día de hoy. Debe registrar su entrada primero."));

        if (registro.getHoraSalida() != null) {
            String horaSalidaStr = registro.getHoraSalida().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            throw new IllegalStateException(estudiante.getNombreCompleto() + " ya registró su salida hoy a las " + horaSalidaStr);
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerTendenciaUltimosDias(int dias) {
        LocalDate fin = FechaUtil.hoyPeru();
        LocalDate inicio = fin.minusDays(dias - 1);
        List<Map<String, Object>> lista = new ArrayList<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            Map<String, Object> item = new HashMap<>();
            long punt = asistenciaRepository.countByFechaAndEstado(d, EstadoPuntualidad.PUNTUAL)
                    + asistenciaRepository.countByFechaAndEstado(d, EstadoPuntualidad.TEMPRANO);
            long tard = asistenciaRepository.countByFechaAndEstado(d, EstadoPuntualidad.TARDANZA);
            long falt = asistenciaRepository.countByFechaAndEstado(d, EstadoPuntualidad.INASISTENCIA);
            long just = asistenciaRepository.countByFechaAndEstado(d, EstadoPuntualidad.JUSTIFICADO);

            item.put("fecha", d.format(fmt));
            item.put("puntuales", punt);
            item.put("tardanzas", tard);
            item.put("inasistencias", falt);
            item.put("justificados", just);
            item.put("total", punt + tard + falt + just);
            lista.add(item);
        }
        return lista;
    }
}
