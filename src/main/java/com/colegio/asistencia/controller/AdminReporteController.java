package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.JustificacionInasistenciaRepository;
import com.colegio.asistencia.repository.RegistroAsistenciaRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReporteController {

    private final RegistroAsistenciaRepository asistenciaRepository;
    private final ConfiguracionColegioRepository configRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AdminReporteController(
            RegistroAsistenciaRepository asistenciaRepository,
            ConfiguracionColegioRepository configRepository,
            JustificacionInasistenciaRepository justificacionRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.configRepository = configRepository;
        this.justificacionRepository = justificacionRepository;
    }

    @ModelAttribute
    public void agregarConfiguracion(Model model) {
        ConfiguracionColegio config = configRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio(
                        "I.E.P. Internacional Jeshua",
                        "Instruye al nino en su camino",
                        "Tuman, Chiclayo, Lambayeque - Peru",
                        "+51 974 123 456",
                        "contacto@internacionaljeshua.edu.pe"
                ));
        model.addAttribute("configColegio", config);
        model.addAttribute("totalJustificacionesPendientes", justificacionRepository.countByEstado(EnumEstadoJustificacion.PENDIENTE));
    }

    @GetMapping
    public String verReporte(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "estado", required = false) String estadoStr,
            Model model) {

        if (desde == null) {
            desde = LocalDate.now().withDayOfMonth(1); // Primer día del mes
        }
        if (hasta == null) {
            hasta = LocalDate.now();
        }

        EstadoPuntualidad estadoFiltro = null;
        if (estadoStr != null && !estadoStr.trim().isEmpty() && !"TODOS".equalsIgnoreCase(estadoStr)) {
            try {
                estadoFiltro = EstadoPuntualidad.valueOf(estadoStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<RegistroAsistencia> resultados = asistenciaRepository.filtrarReportes(desde, hasta, estadoFiltro);

        long total = resultados.size();
        long puntuales = resultados.stream().filter(r -> r.getEstado() == EstadoPuntualidad.PUNTUAL || r.getEstado() == EstadoPuntualidad.TEMPRANO).count();
        long tardanzas = resultados.stream().filter(r -> r.getEstado() == EstadoPuntualidad.TARDANZA).count();
        long faltas = resultados.stream().filter(r -> r.getEstado() == EstadoPuntualidad.INASISTENCIA).count();
        long justificadas = resultados.stream().filter(r -> r.getEstado() == EstadoPuntualidad.JUSTIFICADO).count();

        model.addAttribute("registros", resultados);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("estadoFiltro", estadoStr != null ? estadoStr.toUpperCase() : "TODOS");
        model.addAttribute("total", total);
        model.addAttribute("puntuales", puntuales);
        model.addAttribute("tardanzas", tardanzas);
        model.addAttribute("faltas", faltas);
        model.addAttribute("justificadas", justificadas);

        return "admin/reportes/asistencias";
    }

    @GetMapping("/exportar/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "estado", required = false) String estadoStr) {

        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();

        EstadoPuntualidad estadoFiltro = null;
        if (estadoStr != null && !estadoStr.trim().isEmpty() && !"TODOS".equalsIgnoreCase(estadoStr)) {
            try {
                estadoFiltro = EstadoPuntualidad.valueOf(estadoStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<RegistroAsistencia> resultados = asistenciaRepository.filtrarReportes(desde, hasta, estadoFiltro);

        StringBuilder csv = new StringBuilder();
        // BOM UTF-8 para que Excel lo abra con tildes y caracteres en español correctamente
        csv.append("\uFEFF");
        csv.append("Fecha,DNI,Estudiante,Nivel,Grado,Sección,Turno,Hora Entrada,Hora Salida,Estado,Minutos Tardanza,Observaciones\n");

        for (RegistroAsistencia r : resultados) {
            csv.append("\"").append(r.getFecha().format(dateFormatter)).append("\",");
            csv.append("\"").append(r.getEstudiante().getDni()).append("\",");
            csv.append("\"").append(r.getEstudiante().getNombreCompleto().replace("\"", "\"\"")).append("\",");
            csv.append("\"").append(r.getMatricula() != null ? r.getMatricula().getSeccion().getGrado().getNivel().getNombre() : "-").append("\",");
            csv.append("\"").append(r.getMatricula() != null ? r.getMatricula().getSeccion().getGrado().getNombre() : "-").append("\",");
            csv.append("\"").append(r.getMatricula() != null ? r.getMatricula().getSeccion().getNombre() : "-").append("\",");
            csv.append("\"").append(r.getMatricula() != null ? r.getMatricula().getTurno().getNombre() : "-").append("\",");
            csv.append("\"").append(r.getHoraEntrada() != null ? r.getHoraEntrada().format(timeFormatter) : "-").append("\",");
            csv.append("\"").append(r.getHoraSalida() != null ? r.getHoraSalida().format(timeFormatter) : "-").append("\",");
            csv.append("\"").append(r.getEstado().getDescripcion()).append("\",");
            csv.append(r.getMinutosTardanza()).append(",");
            csv.append("\"").append(r.getObservaciones() != null ? r.getObservaciones().replace("\"", "\"\"") : "").append("\"\n");
        }

        String filename = "Reporte_Asistencias_Jeshua_" + LocalDate.now() + ".csv";
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
