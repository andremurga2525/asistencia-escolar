package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import com.colegio.asistencia.service.EmailNotificacionService;
import com.colegio.asistencia.util.FechaUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/profesor")
@PreAuthorize("hasAnyRole('PROFESOR', 'TUTOR', 'ADMIN')")
public class ProfesorController {

    private final TareaEscolarRepository tareaRepository;
    private final ComunicadoAulaRepository comunicadoRepository;
    private final ReporteConductaRepository reporteConductaRepository;
    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionColegioRepository configuracionRepository;
    private final EmailNotificacionService emailService;

    public ProfesorController(
            TareaEscolarRepository tareaRepository,
            ComunicadoAulaRepository comunicadoRepository,
            ReporteConductaRepository reporteConductaRepository,
            GradoRepository gradoRepository,
            SeccionRepository seccionRepository,
            EstudianteRepository estudianteRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracionColegioRepository configuracionRepository,
            EmailNotificacionService emailService) {
        this.tareaRepository = tareaRepository;
        this.comunicadoRepository = comunicadoRepository;
        this.reporteConductaRepository = reporteConductaRepository;
        this.gradoRepository = gradoRepository;
        this.seccionRepository = seccionRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionRepository = configuracionRepository;
        this.emailService = emailService;
    }

    private ConfiguracionColegio obtenerColegio() {
        return configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("I.E.P. Internacional Jeshua", "Lema", "-", "-", "-"));
    }

    // ==========================================
    // 1. DASHBOARD DEL DOCENTE / TUTOR
    // ==========================================
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        model.addAttribute("usuario", usuario);
        model.addAttribute("colegio", obtenerColegio());

        List<TareaEscolar> tareas = tareaRepository.findAllByOrderByFechaCreacionDesc();
        List<ComunicadoAula> comunicados = comunicadoRepository.findAllByOrderByFechaPublicacionDesc();
        List<ReporteConducta> reportes = reporteConductaRepository.findAllByOrderByFechaCreacionDesc();

        model.addAttribute("tareas", tareas);
        model.addAttribute("comunicados", comunicados);
        model.addAttribute("reportes", reportes);
        model.addAttribute("totalTareas", tareas.size());
        model.addAttribute("totalComunicados", comunicados.size());
        model.addAttribute("totalReportes", reportes.size());

        return "profesor/dashboard";
    }

    // ==========================================
    // 2. TAREAS ESCOLARES (PROFESORES / TUTORES)
    // ==========================================
    @GetMapping("/tareas")
    public String listarTareas(Model model) {
        model.addAttribute("tareas", tareaRepository.findAllByOrderByFechaCreacionDesc());
        model.addAttribute("colegio", obtenerColegio());
        return "profesor/tareas/lista";
    }

    @GetMapping("/tareas/nueva")
    public String nuevaTareaForm(Model model) {
        model.addAttribute("tarea", new TareaEscolar());
        model.addAttribute("grados", gradoRepository.findAll());
        model.addAttribute("secciones", seccionRepository.findAll());
        model.addAttribute("colegio", obtenerColegio());
        return "profesor/tareas/formulario";
    }

    @PostMapping("/tareas/guardar")
    public String guardarTarea(
            @ModelAttribute TareaEscolar tarea,
            @RequestParam("gradoId") Long gradoId,
            @RequestParam("seccionId") Long seccionId,
            @RequestParam("fechaEntregaStr") String fechaEntregaStr,
            @RequestParam(value = "archivoAdjunto", required = false) MultipartFile archivo,
            Authentication auth,
            RedirectAttributes flash) {

        try {
            Usuario profesor = usuarioRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));
            Grado grado = gradoRepository.findById(gradoId)
                    .orElseThrow(() -> new IllegalArgumentException("Grado no válido"));
            Seccion seccion = seccionRepository.findById(seccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no válida"));

            tarea.setProfesor(profesor);
            tarea.setGrado(grado);
            tarea.setSeccion(seccion);
            if (fechaEntregaStr != null && !fechaEntregaStr.trim().isEmpty()) {
                tarea.setFechaEntrega(LocalDate.parse(fechaEntregaStr.trim()));
            }

            if (archivo != null && !archivo.isEmpty()) {
                tarea.setArchivoAdjuntoData(archivo.getBytes());
                tarea.setArchivoAdjuntoNombre(archivo.getOriginalFilename());
                tarea.setArchivoAdjuntoContentType(archivo.getContentType());
            }

            TareaEscolar guardada = tareaRepository.save(tarea);

            // Notificar por correo a los padres de la sección
            try {
                emailService.notificarTareaNueva(guardada);
            } catch (Exception e) {
                // Silencioso para no romper la experiencia si smtp no está configurado
            }

            flash.addFlashAttribute("mensajeExito", "Tarea escolar publicada exitosamente. Se envió copia a los correos de los padres.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", "Error al registrar la tarea: " + e.getMessage());
            return "redirect:/profesor/tareas/nueva";
        }

        return "redirect:/profesor/tareas";
    }

    @GetMapping("/tareas/{id}/adjunto")
    @ResponseBody
    public ResponseEntity<byte[]> descargarAdjuntoTarea(@PathVariable Long id) {
        TareaEscolar tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        if (!tarea.tieneArchivo()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (tarea.getArchivoAdjuntoContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(tarea.getArchivoAdjuntoContentType());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (tarea.getArchivoAdjuntoNombre() != null ? tarea.getArchivoAdjuntoNombre() : "archivo_tarea") + "\"")
                .body(tarea.getArchivoAdjuntoData());
    }

    @PostMapping("/tareas/{id}/eliminar")
    public String eliminarTarea(@PathVariable Long id, RedirectAttributes flash) {
        tareaRepository.deleteById(id);
        flash.addFlashAttribute("mensajeExito", "Tarea eliminada correctamente.");
        return "redirect:/profesor/tareas";
    }

    // ==========================================
    // 3. COMUNICADOS DE AULA (TUTORES)
    // ==========================================
    @GetMapping("/comunicados")
    public String listarComunicados(Model model) {
        model.addAttribute("comunicados", comunicadoRepository.findAllByOrderByFechaPublicacionDesc());
        model.addAttribute("colegio", obtenerColegio());
        return "profesor/comunicados/lista";
    }

    @GetMapping("/comunicados/nuevo")
    public String nuevoComunicadoForm(Model model) {
        model.addAttribute("comunicado", new ComunicadoAula());
        model.addAttribute("grados", gradoRepository.findAll());
        model.addAttribute("secciones", seccionRepository.findAll());
        model.addAttribute("colegio", obtenerColegio());
        return "profesor/comunicados/formulario";
    }

    @PostMapping("/comunicados/guardar")
    public String guardarComunicado(
            @ModelAttribute ComunicadoAula comunicado,
            @RequestParam(value = "gradoId", required = false) Long gradoId,
            @RequestParam(value = "seccionId", required = false) Long seccionId,
            @RequestParam(value = "archivoAdjunto", required = false) MultipartFile archivo,
            Authentication auth,
            RedirectAttributes flash) {

        try {
            Usuario tutor = usuarioRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));

            comunicado.setTutor(tutor);
            if (gradoId != null && gradoId > 0) {
                comunicado.setGrado(gradoRepository.findById(gradoId).orElse(null));
            }
            if (seccionId != null && seccionId > 0) {
                comunicado.setSeccion(seccionRepository.findById(seccionId).orElse(null));
            }

            if (archivo != null && !archivo.isEmpty()) {
                comunicado.setArchivoAdjuntoData(archivo.getBytes());
                comunicado.setArchivoAdjuntoNombre(archivo.getOriginalFilename());
                comunicado.setArchivoAdjuntoContentType(archivo.getContentType());
            }

            ComunicadoAula guardado = comunicadoRepository.save(comunicado);

            // Notificar por correo
            try {
                emailService.notificarComunicado(guardado);
            } catch (Exception ignored) {}

            flash.addFlashAttribute("mensajeExito", "Comunicado oficial publicado con éxito. Notificaciones enviadas.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", "Error al publicar comunicado: " + e.getMessage());
            return "redirect:/profesor/comunicados/nuevo";
        }

        return "redirect:/profesor/comunicados";
    }

    @GetMapping("/comunicados/{id}/adjunto")
    @ResponseBody
    public ResponseEntity<byte[]> descargarAdjuntoComunicado(@PathVariable Long id) {
        ComunicadoAula com = comunicadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comunicado no encontrado"));

        if (!com.tieneArchivo()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (com.getArchivoAdjuntoContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(com.getArchivoAdjuntoContentType());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (com.getArchivoAdjuntoNombre() != null ? com.getArchivoAdjuntoNombre() : "comunicado_adjunto") + "\"")
                .body(com.getArchivoAdjuntoData());
    }

    @PostMapping("/comunicados/{id}/eliminar")
    public String eliminarComunicado(@PathVariable Long id, RedirectAttributes flash) {
        comunicadoRepository.deleteById(id);
        flash.addFlashAttribute("mensajeExito", "Comunicado eliminado.");
        return "redirect:/profesor/comunicados";
    }

    // ==========================================
    // 4. REPORTES DE CONDUCTA / DISCIPLINA (TUTORES)
    // ==========================================
    @GetMapping("/conducta")
    public String listarReportesConducta(Model model) {
        model.addAttribute("reportes", reporteConductaRepository.findAllByOrderByFechaCreacionDesc());
        model.addAttribute("colegio", obtenerColegio());
        return "profesor/conducta/lista";
    }

    @GetMapping("/conducta/nuevo")
    public String nuevoReporteConductaForm(Model model) {
        model.addAttribute("reporte", new ReporteConducta());
        model.addAttribute("estudiantes", estudianteRepository.findAll());
        model.addAttribute("colegio", obtenerColegio());
        return "profesor/conducta/formulario";
    }

    @PostMapping("/conducta/guardar")
    public String guardarReporteConducta(
            @ModelAttribute ReporteConducta reporte,
            @RequestParam("estudianteId") Long estudianteId,
            @RequestParam(value = "requiereCitacionBool", required = false, defaultValue = "false") boolean citacion,
            @RequestParam(value = "fechaCitacionStr", required = false) String fechaCitacionStr,
            Authentication auth,
            RedirectAttributes flash) {

        try {
            Usuario tutor = usuarioRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));
            Estudiante estudiante = estudianteRepository.findById(estudianteId)
                    .orElseThrow(() -> new IllegalArgumentException("Estudiante no válido"));

            reporte.setTutor(tutor);
            reporte.setEstudiante(estudiante);
            reporte.setRequiereCitacion(citacion);

            if (citacion && fechaCitacionStr != null && !fechaCitacionStr.trim().isEmpty()) {
                reporte.setFechaCitacion(LocalDateTime.parse(fechaCitacionStr.trim()));
            }

            ReporteConducta guardado = reporteConductaRepository.save(reporte);

            // Enviar copia por correo al apoderado
            try {
                emailService.notificarReporteConducta(guardado);
            } catch (Exception ignored) {}

            flash.addFlashAttribute("mensajeExito", "Reporte de conducta registrado y notificado al apoderado de " + estudiante.getNombreCompleto());
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", "Error al registrar reporte de conducta: " + e.getMessage());
            return "redirect:/profesor/conducta/nuevo";
        }

        return "redirect:/profesor/conducta";
    }

    @PostMapping("/conducta/{id}/eliminar")
    public String eliminarReporteConducta(@PathVariable Long id, RedirectAttributes flash) {
        reporteConductaRepository.deleteById(id);
        flash.addFlashAttribute("mensajeExito", "Reporte de conducta eliminado.");
        return "redirect:/profesor/conducta";
    }
}
