package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import com.colegio.asistencia.service.EmailNotificacionService;
import com.colegio.asistencia.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/justificaciones")
@PreAuthorize("hasRole('ADMIN')")
public class AdminJustificacionController {

    private final JustificacionInasistenciaRepository justificacionRepository;
    private final RegistroAsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionColegioRepository configRepository;
    private final FileStorageService fileStorageService;
    private final EmailNotificacionService emailService;

    public AdminJustificacionController(
            JustificacionInasistenciaRepository justificacionRepository,
            RegistroAsistenciaRepository asistenciaRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracionColegioRepository configRepository,
            FileStorageService fileStorageService,
            @Autowired(required = false) EmailNotificacionService emailService) {
        this.justificacionRepository = justificacionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.configRepository = configRepository;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
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
    public String listar(@RequestParam(value = "estado", required = false) String estadoFiltro, Model model) {
        List<JustificacionInasistencia> lista;

        if ("PENDIENTE".equalsIgnoreCase(estadoFiltro)) {
            lista = justificacionRepository.findByEstadoOrderByFechaSolicitudDesc(EnumEstadoJustificacion.PENDIENTE);
        } else if ("APROBADA".equalsIgnoreCase(estadoFiltro)) {
            lista = justificacionRepository.findByEstadoOrderByFechaSolicitudDesc(EnumEstadoJustificacion.APROBADA);
        } else if ("RECHAZADA".equalsIgnoreCase(estadoFiltro)) {
            lista = justificacionRepository.findByEstadoOrderByFechaSolicitudDesc(EnumEstadoJustificacion.RECHAZADA);
        } else {
            lista = justificacionRepository.findAllByOrderByFechaSolicitudDesc();
            estadoFiltro = "TODAS";
        }

        model.addAttribute("justificaciones", lista);
        model.addAttribute("filtroActivo", estadoFiltro.toUpperCase());
        model.addAttribute("totalPendientes", justificacionRepository.countByEstado(EnumEstadoJustificacion.PENDIENTE));
        model.addAttribute("totalAprobadas", justificacionRepository.countByEstado(EnumEstadoJustificacion.APROBADA));
        model.addAttribute("totalRechazadas", justificacionRepository.countByEstado(EnumEstadoJustificacion.RECHAZADA));
        model.addAttribute("totalGeneral", justificacionRepository.count());

        return "admin/justificaciones/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        JustificacionInasistencia justificacion = justificacionRepository.findById(id).orElse(null);
        if (justificacion == null) {
            flash.addFlashAttribute("error", "La solicitud de justificación no fue encontrada.");
            return "redirect:/admin/justificaciones";
        }

        boolean tieneArchivo = justificacion.getArchivoEvidencia() != null && !justificacion.getArchivoEvidencia().trim().isEmpty();
        boolean esPdf = tieneArchivo && justificacion.getArchivoEvidencia().toLowerCase().endsWith(".pdf");
        boolean esImagen = tieneArchivo && (justificacion.getArchivoEvidencia().toLowerCase().endsWith(".jpg")
                || justificacion.getArchivoEvidencia().toLowerCase().endsWith(".jpeg")
                || justificacion.getArchivoEvidencia().toLowerCase().endsWith(".png"));

        model.addAttribute("justificacion", justificacion);
        model.addAttribute("tieneArchivo", tieneArchivo);
        model.addAttribute("esPdf", esPdf);
        model.addAttribute("esImagen", esImagen);

        return "admin/justificaciones/detalle";
    }

    @GetMapping("/{id}/evidencia")
    public ResponseEntity<Resource> verEvidencia(@PathVariable("id") Long id) {
        JustificacionInasistencia justificacion = justificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Justificación no encontrada"));

        String nombreArchivo = justificacion.getArchivoEvidencia();
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso = fileStorageService.cargarComoRecurso(nombreArchivo);
        String contentType = fileStorageService.determinarContentType(nombreArchivo);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                .body(recurso);
    }

    @PostMapping("/{id}/aprobar")
    public String aprobarJustificacion(
            @PathVariable("id") Long id,
            @RequestParam(value = "observacion", required = false) String observacion,
            Authentication auth,
            RedirectAttributes flash) {

        JustificacionInasistencia justificacion = justificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Justificación no encontrada"));

        Usuario revisor = usuarioRepository.findByUsername(auth.getName()).orElse(null);

        justificacion.setEstado(EnumEstadoJustificacion.APROBADA);
        justificacion.setObservacionRevision(observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Justificación aprobada por la Dirección.");
        justificacion.setRevisadoPor(revisor);
        justificacion.setFechaRevision(LocalDateTime.now());
        justificacionRepository.save(justificacion);

        // Actualizar el estado de la asistencia a JUSTIFICADO
        RegistroAsistencia registro = justificacion.getRegistroAsistencia();
        registro.setEstado(EstadoPuntualidad.JUSTIFICADO);
        registro.setObservaciones("Inasistencia justificada formalmente. ID Justif: #" + justificacion.getId());
        asistenciaRepository.save(registro);

        if (emailService != null) {
            try {
                emailService.notificarResolucionJustificacion(justificacion);
            } catch (Exception ignored) {}
        }

        flash.addFlashAttribute("success", "Justificación #" + id + " aprobada exitosamente. La asistencia del alumno ha sido actualizada a 'JUSTIFICADA'.");
        return "redirect:/admin/justificaciones/" + id;
    }

    @PostMapping("/{id}/rechazar")
    public String rechazarJustificacion(
            @PathVariable("id") Long id,
            @RequestParam("motivoRechazo") String motivoRechazo,
            Authentication auth,
            RedirectAttributes flash) {

        if (motivoRechazo == null || motivoRechazo.trim().isEmpty()) {
            flash.addFlashAttribute("error", "Debe indicar el motivo por el cual rechaza la justificación.");
            return "redirect:/admin/justificaciones/" + id;
        }

        JustificacionInasistencia justificacion = justificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Justificación no encontrada"));

        Usuario revisor = usuarioRepository.findByUsername(auth.getName()).orElse(null);

        justificacion.setEstado(EnumEstadoJustificacion.RECHAZADA);
        justificacion.setObservacionRevision(motivoRechazo.trim());
        justificacion.setRevisadoPor(revisor);
        justificacion.setFechaRevision(LocalDateTime.now());
        justificacionRepository.save(justificacion);

        if (emailService != null) {
            try {
                emailService.notificarResolucionJustificacion(justificacion);
            } catch (Exception ignored) {}
        }

        flash.addFlashAttribute("success", "Justificación #" + id + " rechazada con el motivo especificado.");
        return "redirect:/admin/justificaciones/" + id;
    }
}
