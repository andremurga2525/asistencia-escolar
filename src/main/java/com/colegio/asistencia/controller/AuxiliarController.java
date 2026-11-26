package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.model.RegistroAsistencia;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.RegistroAsistenciaRepository;
import com.colegio.asistencia.service.AsistenciaService;
import com.colegio.asistencia.util.FechaUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/auxiliar")
@PreAuthorize("hasAnyRole('AUXILIAR', 'ADMIN')")
public class AuxiliarController {

    private final AsistenciaService asistenciaService;
    private final RegistroAsistenciaRepository asistenciaRepository;
    private final ConfiguracionColegioRepository configuracionRepository;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AuxiliarController(
            AsistenciaService asistenciaService,
            RegistroAsistenciaRepository asistenciaRepository,
            ConfiguracionColegioRepository configuracionRepository) {
        this.asistenciaService = asistenciaService;
        this.asistenciaRepository = asistenciaRepository;
        this.configuracionRepository = configuracionRepository;
    }

    @GetMapping("/escaner")
    public String escaner(Model model) {
        LocalDate hoy = FechaUtil.hoyPeru();
        List<RegistroAsistencia> escaneosHoy = asistenciaRepository.findByFechaOrderByHoraEntradaAsc(hoy);

        ConfiguracionColegio colegio = configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("I.E.P. Internacional Jeshua", "Lema", "-", "-", "-"));

        model.addAttribute("colegio", colegio);
        model.addAttribute("escaneosHoy", escaneosHoy);
        model.addAttribute("fechaHoy", hoy);

        return "auxiliar/escaner";
    }

    /**
     * Endpoint API JSON de alta velocidad para escaneo continuo con cámara web y lector óptico.
     * No recarga la página, manteniendo la cámara activa y transmitiendo fluidamente.
     */
    @PostMapping("/api/escanear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> procesarEscaneoApi(
            @RequestBody Map<String, String> payload,
            Authentication auth) {

        String codigoQr = payload != null ? payload.get("codigoQr") : null;
        String tipoOperacion = payload != null ? payload.getOrDefault("tipoOperacion", "ENTRADA") : "ENTRADA";

        Map<String, Object> respuesta = new HashMap<>();

        if (codigoQr == null || codigoQr.trim().isEmpty()) {
            respuesta.put("success", false);
            respuesta.put("mensaje", "No se detectó un código QR válido.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        try {
            if ("SALIDA".equalsIgnoreCase(tipoOperacion)) {
                RegistroAsistencia salida = asistenciaService.registrarSalidaPorQr(codigoQr.trim(), auth.getName());
                respuesta.put("success", true);
                respuesta.put("tipoOperacion", "SALIDA");
                respuesta.put("alumnoNombre", salida.getEstudiante().getNombreCompleto());
                respuesta.put("alumnoDni", salida.getEstudiante().getDni());
                respuesta.put("alumnoGrado", salida.getMatricula() != null ? salida.getMatricula().getSeccion().getGrado().getNombre() + " (" + salida.getMatricula().getSeccion().getNombre() + ")" : "General");
                respuesta.put("hora", salida.getHoraSalida() != null ? salida.getHoraSalida().format(timeFormatter) : "Hora registrada");
                respuesta.put("estado", "SALIDA");
                respuesta.put("estadoDescripcion", "Salida Registrada");
                respuesta.put("minutosTardanza", 0);
                respuesta.put("mensaje", "Salida registrada con éxito a las " + salida.getHoraSalida().format(timeFormatter));
            } else {
                RegistroAsistencia entrada = asistenciaService.registrarEntradaPorQr(codigoQr.trim(), auth.getName());
                respuesta.put("success", true);
                respuesta.put("tipoOperacion", "ENTRADA");
                respuesta.put("alumnoNombre", entrada.getEstudiante().getNombreCompleto());
                respuesta.put("alumnoDni", entrada.getEstudiante().getDni());
                respuesta.put("alumnoGrado", entrada.getMatricula() != null ? entrada.getMatricula().getSeccion().getGrado().getNombre() + " (" + entrada.getMatricula().getSeccion().getNombre() + ")" : "General");
                respuesta.put("hora", entrada.getHoraEntrada() != null ? entrada.getHoraEntrada().format(timeFormatter) : "Hora registrada");
                respuesta.put("estado", entrada.getEstado().name());
                respuesta.put("estadoDescripcion", entrada.getEstado().getDescripcion());
                respuesta.put("minutosTardanza", entrada.getMinutosTardanza());
                String msg = "Asistencia registrada: " + entrada.getEstado().getDescripcion();
                if (entrada.getMinutosTardanza() > 0) {
                    msg += " (" + entrada.getMinutosTardanza() + " min de tardanza)";
                }
                respuesta.put("mensaje", msg);
            }
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            respuesta.put("success", false);
            respuesta.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);
        }
    }

    @PostMapping("/escanear")
    public String procesarEscaneoForm(
            @RequestParam String codigoQr,
            @RequestParam(defaultValue = "ENTRADA") String tipoOperacion,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            if ("SALIDA".equalsIgnoreCase(tipoOperacion)) {
                RegistroAsistencia salida = asistenciaService.registrarSalidaPorQr(codigoQr, auth.getName());
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Salida registrada: " + salida.getEstudiante().getNombreCompleto() + " a las " + salida.getHoraSalida());
            } else {
                RegistroAsistencia entrada = asistenciaService.registrarEntradaPorQr(codigoQr, auth.getName());
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Asistencia registrada: " + entrada.getEstudiante().getNombreCompleto() +
                                " | Estado: " + entrada.getEstado().getDescripcion() +
                                (entrada.getMinutosTardanza() > 0 ? " (" + entrada.getMinutosTardanza() + " min tardanza)" : ""));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }

        return "redirect:/auxiliar/escaner";
    }
}
