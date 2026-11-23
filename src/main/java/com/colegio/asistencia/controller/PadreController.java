package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import com.colegio.asistencia.service.FileStorageService;
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

    public PadreController(
            ApoderadoRepository apoderadoRepository,
            RegistroAsistenciaRepository asistenciaRepository,
            JustificacionInasistenciaRepository justificacionRepository,
            ConfiguracionColegioRepository configuracionRepository,
            FileStorageService fileStorageService,
            EstudianteRepository estudianteRepository) {
        this.apoderadoRepository = apoderadoRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.justificacionRepository = justificacionRepository;
        this.configuracionRepository = configuracionRepository;
        this.fileStorageService = fileStorageService;
        this.estudianteRepository = estudianteRepository;
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

        // Historial de asistencias de los hijos
        Map<Long, List<RegistroAsistencia>> asistenciasPorHijo = new HashMap<>();
        Map<Long, Boolean> plazoValidoPorRegistro = new HashMap<>();
        Map<Long, JustificacionInasistencia> justificacionesPorRegistro = new HashMap<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (Estudiante hijo : hijosFiltrados) {
            List<RegistroAsistencia> asistencias = asistenciaRepository.findByEstudianteDniOrderByFechaDesc(hijo.getDni());
            asistenciasPorHijo.put(hijo.getId(), asistencias);

            for (RegistroAsistencia asis : asistencias) {
                // Verificar si ya tiene justificación
                justificacionRepository.findByRegistroAsistenciaId(asis.getId())
                        .ifPresent(j -> justificacionesPorRegistro.put(asis.getId(), j));

                if (asis.getEstado() == EstadoPuntualidad.INASISTENCIA) {
                    LocalDateTime limite = asis.getFecha().atTime(23, 59, 59).plusHours(48);
                    boolean enPlazo = !ahora.isAfter(limite);
                    plazoValidoPorRegistro.put(asis.getId(), enPlazo);
                }
            }
        }

        ConfiguracionColegio colegio = configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("I.E.P. Internacional Jeshua", "Lema", "-", "-", "-"));

        model.addAttribute("colegio", colegio);
        model.addAttribute("apoderado", apoderado);
        model.addAttribute("hijos", hijosFiltrados);
        model.addAttribute("asistenciasPorHijo", asistenciasPorHijo);
        model.addAttribute("plazoValidoPorRegistro", plazoValidoPorRegistro);
        model.addAttribute("justificacionesPorRegistro", justificacionesPorRegistro);

        return "padre/hijos";
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

        // Seguridad: Verificar que el estudiante realmente sea hijo del apoderado solicitante
        if (!apoderado.tieneHijoConDni(registro.getEstudiante().getDni())) {
            throw new SecurityException("No tiene autorización para justificar la inasistencia de este estudiante.");
        }

        // Seguridad estricta: Verificar la ventana de 48 horas calculada por el servidor
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaLimite = registro.getFecha().atTime(23, 59, 59).plusHours(48);

        if (ahora.isAfter(fechaLimite)) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El plazo legal de 48 horas para justificar esta inasistencia del " + registro.getFecha() + " ha vencido.");
            return "redirect:/padre/hijos";
        }

        // Verificar si ya existe una justificación para este registro
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
