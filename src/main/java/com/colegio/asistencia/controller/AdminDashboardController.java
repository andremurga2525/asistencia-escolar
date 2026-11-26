package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.JustificacionInasistenciaRepository;
import com.colegio.asistencia.repository.RegistroAsistenciaRepository;
import com.colegio.asistencia.service.AsistenciaService;
import com.colegio.asistencia.util.FechaUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AsistenciaService asistenciaService;
    private final RegistroAsistenciaRepository asistenciaRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;
    private final ConfiguracionColegioRepository configuracionRepository;

    public AdminDashboardController(
            AsistenciaService asistenciaService,
            RegistroAsistenciaRepository asistenciaRepository,
            JustificacionInasistenciaRepository justificacionRepository,
            ConfiguracionColegioRepository configuracionRepository) {
        this.asistenciaService = asistenciaService;
        this.asistenciaRepository = asistenciaRepository;
        this.justificacionRepository = justificacionRepository;
        this.configuracionRepository = configuracionRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate hoy = FechaUtil.hoyPeru();

        // 1. Estadísticas calculadas directamente de MySQL 8.4
        Map<String, Object> stats = asistenciaService.obtenerEstadisticasDashboard(hoy);
        model.addAttribute("stats", stats);

        // 2. Registros de asistencia de hoy
        List<RegistroAsistencia> asistenciasHoy = asistenciaRepository.findByFechaOrderByHoraEntradaAsc(hoy);
        model.addAttribute("asistenciasHoy", asistenciasHoy);

        // 3. Justificaciones pendientes de revisión
        List<JustificacionInasistencia> pendientes = justificacionRepository.findByEstado(EnumEstadoJustificacion.PENDIENTE);
        model.addAttribute("justificacionesPendientes", pendientes);
        model.addAttribute("totalJustificacionesPendientes", pendientes.size());

        // 4. Configuración del colegio
        ConfiguracionColegio colegio = configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("I.E.P. Internacional Jeshua", "Lema", "-", "-", "-"));
        model.addAttribute("colegio", colegio);
        model.addAttribute("configColegio", colegio);
        model.addAttribute("fechaHoy", hoy);

        return "admin/dashboard";
    }
}
