package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import com.colegio.asistencia.service.FileStorageService;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/padre")
@PreAuthorize("hasRole('PADRE')")
public class PadreController {

    private final ApoderadoRepository apoderadoRepository;
    private final RegistroAsistenciaRepository asistenciaRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;
    private final ConfiguracionColegioRepository configuracionRepository;
    private final FileStorageService fileStorageService;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final TareaEscolarRepository tareaRepository;
    private final ComunicadoAulaRepository comunicadoRepository;
    private final ReporteConductaRepository reporteConductaRepository;

    public PadreController(
            ApoderadoRepository apoderadoRepository,
            RegistroAsistenciaRepository asistenciaRepository,
            JustificacionInasistenciaRepository justificacionRepository,
            ConfiguracionColegioRepository configuracionRepository,
            FileStorageService fileStorageService,
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            TareaEscolarRepository tareaRepository,
            ComunicadoAulaRepository comunicadoRepository,
            ReporteConductaRepository reporteConductaRepository) {
        this.apoderadoRepository = apoderadoRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.justificacionRepository = justificacionRepository;
        this.configuracionRepository = configuracionRepository;
        this.fileStorageService = fileStorageService;
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.tareaRepository = tareaRepository;
        this.comunicadoRepository = comunicadoRepository;
        this.reporteConductaRepository = reporteConductaRepository;
    }

    @GetMapping("/hijos")
    public String verHijos(
            @RequestParam(required = false) String dniHijo,
            Authentication auth,
            Model model) {

        String dniPadre = auth.getName();
        Apoderado apoderado = apoderadoRepository.findByDni(dniPadre)
                .orElseThrow(() -> new IllegalStateException("Apoderado no encontrado con DNI: " + dniPadre));

        Set<Estudiante> hijos = apoderado.getEstudiantes();

        // Si el padre busca un hijo específico por DNI
        List<Estudiante> hijosFiltrados;
        if (dniHijo != null && !dniHijo.trim().isEmpty()) {
            hijosFiltrados = hijos.stream()
                    .filter(h -> h.getDni().equalsIgnoreCase(dniHijo.trim()))
                    .collect(Collectors.toList());
            model.addAttribute("busquedaDni", dniHijo.trim());
        } else {
            hijosFiltrados = new ArrayList<>(hijos);
        }

        // Mapas por hijo: asistencias, tareas, comunicados y conducta
        Map<Long, List<RegistroAsistencia>> asistenciasPorHijo = new HashMap<>();
        Map<Long, Boolean> plazoValidoPorRegistro = new HashMap<>();
        Map<Long, JustificacionInasistencia> justificacionesPorRegistro = new HashMap<>();
        Map<Long, List<TareaEscolar>> tareasPorHijo = new HashMap<>();
        Map<Long, List<ComunicadoAula>> comunicadosPorHijo = new HashMap<>();
        Map<Long, List<ReporteConducta>> reportesPorHijo = new HashMap<>();
        Map<Long, Matricula> matriculaPorHijo = new HashMap<>();

        LocalDateTime ahora = FechaUtil.ahoraFechaHoraPeru();
        int totalTareasPendientes = 0;
        int totalComunicadosUrgentes = 0;
        List<String> alertasPush = new ArrayList<>();

        for (Estudiante hijo : hijosFiltrados) {
            // Asistencias
            List<RegistroAsistencia> asistencias = asistenciaRepository.findByEstudianteDniOrderByFechaDesc(hijo.getDni());
            asistenciasPorHijo.put(hijo.getId(), asistencias);

            for (RegistroAsistencia asis : asistencias) {
                justificacionRepository.findByRegistroAsistenciaId(asis.getId())
                        .ifPresent(j -> justificacionesPorRegistro.put(asis.getId(), j));

                if (asis.getEstado() == EstadoPuntualidad.INASISTENCIA) {
                    LocalDateTime limite = asis.getFecha().atTime(23, 59, 59).plusHours(48);
                    boolean enPlazo = !ahora.isAfter(limite);
                    plazoValidoPorRegistro.put(asis.getId(), enPlazo);
                }
            }

            // Matrícula, Tareas y Comunicados
            Optional<Matricula> matOpt = matriculaRepository.findByEstudianteIdAndAnioEscolarActivoTrue(hijo.getId());
            if (matOpt.isPresent()) {
                Matricula mat = matOpt.get();
                matriculaPorHijo.put(hijo.getId(), mat);

                if (mat.getSeccion() != null && mat.getSeccion().getGrado() != null) {
                    Long gradoId = mat.getSeccion().getGrado().getId();
                    Long seccionId = mat.getSeccion().getId();

                    List<TareaEscolar> tareas = tareaRepository.findByGradoIdAndSeccionIdAndActivoTrueOrderByFechaEntregaAsc(gradoId, seccionId);
                    tareasPorHijo.put(hijo.getId(), tareas);
                    for (TareaEscolar t : tareas) {
                        if (!t.isVencida()) {
                            totalTareasPendientes++;
                            alertasPush.add("Tarea: " + t.getMateria() + " - " + t.getTitulo() + " (Para: " + hijo.getNombreCompleto() + ")");
                        }
                    }

                    List<ComunicadoAula> coms = comunicadoRepository.findByGradoIdAndSeccionIdOrderByFechaPublicacionDesc(gradoId, seccionId);
                    comunicadosPorHijo.put(hijo.getId(), coms);
                    for (ComunicadoAula c : coms) {
                        if (c.isUrgente()) {
                            totalComunicadosUrgentes++;
                            alertasPush.add("COMUNICADO URGENTE: " + c.getTitulo());
                        }
                    }
                }
            }

            // Reportes de Conducta
            List<ReporteConducta> reportes = reporteConductaRepository.findByEstudianteIdOrderByFechaIncidenciaDesc(hijo.getId());
            reportesPorHijo.put(hijo.getId(), reportes);
        }

        ConfiguracionColegio colegio = configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("I.E.P. Internacional Jeshua", "Lema", "-", "-", "-"));

        model.addAttribute("colegio", colegio);
        model.addAttribute("apoderado", apoderado);
        model.addAttribute("hijos", hijosFiltrados);
        model.addAttribute("asistenciasPorHijo", asistenciasPorHijo);
        model.addAttribute("plazoValidoPorRegistro", plazoValidoPorRegistro);
        model.addAttribute("justificacionesPorRegistro", justificacionesPorRegistro);
        model.addAttribute("matriculaPorHijo", matriculaPorHijo);
        model.addAttribute("tareasPorHijo", tareasPorHijo);
        model.addAttribute("comunicadosPorHijo", comunicadosPorHijo);
        model.addAttribute("reportesPorHijo", reportesPorHijo);
        model.addAttribute("totalTareasPendientes", totalTareasPendientes);
        model.addAttribute("totalComunicadosUrgentes", totalComunicadosUrgentes);
        model.addAttribute("alertasPush", alertasPush);

        return "padre/hijos";
    }

    @PostMapping("/actualizar-parentesco")
    public String actualizarParentesco(
            @RequestParam("parentesco") String parentesco,
            Authentication auth,
            RedirectAttributes flash) {

        String dniPadre = auth.getName();
        Apoderado apoderado = apoderadoRepository.findByDni(dniPadre)
                .orElseThrow(() -> new IllegalStateException("Apoderado no encontrado con DNI: " + dniPadre));

        if (parentesco != null && !parentesco.trim().isEmpty()) {
            apoderado.setParentesco(parentesco.trim());
            apoderadoRepository.save(apoderado);
            flash.addFlashAttribute("mensajeExito", "Parentesco actualizado a: " + ("MADRE".equalsIgnoreCase(parentesco) ? "Madre" : ("PADRE".equalsIgnoreCase(parentesco) ? "Padre" : "Apoderado Legal")));
        }

        return "redirect:/padre/hijos";
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (tarea.getArchivoAdjuntoNombre() != null ? tarea.getArchivoAdjuntoNombre() : "adjunto_tarea") + "\"")
                .body(tarea.getArchivoAdjuntoData());
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

    @PostMapping("/justificar")
    public String presentarJustificacion(
            @RequestParam Long registroId,
            @RequestParam String motivo,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        String dniPadre = auth.getName();
        Apoderado apoderado = apoderadoRepository.findByDni(dniPadre)
                .orElseThrow(() -> new IllegalStateException("Apoderado no encontrado con DNI: " + dniPadre));

        RegistroAsistencia registro = asistenciaRepository.findById(registroId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado."));

        if (!apoderado.tieneHijoConDni(registro.getEstudiante().getDni())) {
            throw new SecurityException("No tiene autorización para justificar la inasistencia de este estudiante.");
        }

        LocalDateTime ahora = FechaUtil.ahoraFechaHoraPeru();
        LocalDateTime fechaLimite = registro.getFecha().atTime(23, 59, 59).plusHours(48);

        if (ahora.isAfter(fechaLimite)) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El plazo legal de 48 horas para justificar esta inasistencia del " + registro.getFecha() + " ha vencido.");
            return "redirect:/padre/hijos";
        }

        if (justificacionRepository.findByRegistroAsistenciaId(registroId).isPresent()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Ya existe una justificación registrada para esta fecha.");
            return "redirect:/padre/hijos";
        }

        String nombreArchivoGuardado = null;
        if (archivo != null && !archivo.isEmpty()) {
            try {
                nombreArchivoGuardado = fileStorageService.almacenarArchivo(archivo);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("mensajeError", "Error al subir el archivo: " + e.getMessage());
                return "redirect:/padre/hijos";
            }
        }

        JustificacionInasistencia justificacion = new JustificacionInasistencia(
                registro,
                apoderado,
                motivo,
                nombreArchivoGuardado
        );
        justificacionRepository.save(justificacion);

        redirectAttributes.addFlashAttribute("mensajeExito",
                "Justificación y evidencia enviadas correctamente para " + registro.getEstudiante().getNombreCompleto() +
                        ". La dirección del colegio revisará la solicitud.");

        return "redirect:/padre/hijos";
    }

    @PostMapping("/vincular-hijo")
    public String vincularHijo(
            @RequestParam("dniHijo") String dniHijo,
            Authentication auth,
            RedirectAttributes flash) {

        String dniPadre = auth.getName();
        Apoderado apoderado = apoderadoRepository.findByDni(dniPadre)
                .orElseThrow(() -> new IllegalStateException("Apoderado no encontrado con DNI: " + dniPadre));

        String dniLimpio = dniHijo != null ? dniHijo.trim() : "";
        if (!dniLimpio.matches("^[0-9]{8}$")) {
            flash.addFlashAttribute("mensajeError", "El DNI ingresado no es válido (debe tener 8 dígitos numéricos).");
            return "redirect:/padre/hijos";
        }

        Optional<Estudiante> estOpt = estudianteRepository.findByDni(dniLimpio);
        if (estOpt.isEmpty()) {
            flash.addFlashAttribute("mensajeError", "No se encontró ningún estudiante matriculado con el DNI: " + dniLimpio + ". Por favor consulte con el colegio.");
            return "redirect:/padre/hijos";
        }

        Estudiante est = estOpt.get();
        if (apoderado.getEstudiantes().contains(est)) {
            flash.addFlashAttribute("mensajeError", "El estudiante " + est.getNombreCompleto() + " ya se encuentra vinculado a su cuenta.");
            return "redirect:/padre/hijos";
        }

        apoderado.agregarEstudiante(est);
        apoderadoRepository.save(apoderado);

        flash.addFlashAttribute("mensajeExito", "¡Estudiante " + est.getNombreCompleto() + " vinculado exitosamente a su cuenta!");
        return "redirect:/padre/hijos";
    }
}
