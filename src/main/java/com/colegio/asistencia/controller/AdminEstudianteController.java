package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.SeccionRepository;
import com.colegio.asistencia.repository.TurnoRepository;
import com.colegio.asistencia.service.EstudianteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/estudiantes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEstudianteController {

    private final EstudianteService estudianteService;
    private final SeccionRepository seccionRepository;
    private final TurnoRepository turnoRepository;
    private final ConfiguracionColegioRepository configRepository;

    public AdminEstudianteController(EstudianteService estudianteService,
                                     SeccionRepository seccionRepository,
                                     TurnoRepository turnoRepository,
                                     ConfiguracionColegioRepository configRepository) {
        this.estudianteService = estudianteService;
        this.seccionRepository = seccionRepository;
        this.turnoRepository = turnoRepository;
        this.configRepository = configRepository;
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
    }

    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String busqueda, Model model) {
        List<Estudiante> estudiantes = estudianteService.buscar(busqueda);
        
        // Mapear matricula actual de cada estudiante para mostrar grado y seccion en la tabla
        Map<Long, Matricula> matriculasPorEstudiante = new HashMap<>();
        for (Estudiante est : estudiantes) {
            estudianteService.obtenerMatriculaActual(est.getId())
                    .ifPresent(mat -> matriculasPorEstudiante.put(est.getId(), mat));
        }

        model.addAttribute("estudiantes", estudiantes);
        model.addAttribute("matriculas", matriculasPorEstudiante);
        model.addAttribute("busqueda", busqueda != null ? busqueda : "");
        model.addAttribute("totalEstudiantes", estudiantes.size());
        return "admin/estudiantes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevoFormulario(Model model) {
        Estudiante nuevo = new Estudiante();
        nuevo.setActivo(true);
        model.addAttribute("estudiante", nuevo);
        model.addAttribute("secciones", seccionRepository.findAll());
        model.addAttribute("turnos", turnoRepository.findAll());
        model.addAttribute("esEdicion", false);
        return "admin/estudiantes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("estudiante") Estudiante estudiante,
                          BindingResult bindingResult,
                          @RequestParam(value = "seccionId", required = false) Long seccionId,
                          @RequestParam(value = "turnoId", required = false) Long turnoId,
                          Model model,
                          RedirectAttributes flash) {
        // Validar DNI duplicado
        if (estudiante.getId() == null && estudianteService.existeDni(estudiante.getDni())) {
            bindingResult.rejectValue("dni", "error.estudiante", "Ya existe un estudiante registrado con este DNI");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("secciones", seccionRepository.findAll());
            model.addAttribute("turnos", turnoRepository.findAll());
            model.addAttribute("esEdicion", false);
            return "admin/estudiantes/formulario";
        }

        estudianteService.guardarNuevoEstudiante(estudiante, seccionId, turnoId);
        flash.addFlashAttribute("success", "Estudiante '" + estudiante.getNombreCompleto() + "' registrado exitosamente.");
        return "redirect:/admin/estudiantes";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Estudiante est = estudianteService.obtenerPorId(id).orElse(null);
        if (est == null) {
            flash.addFlashAttribute("error", "Estudiante no encontrado.");
            return "redirect:/admin/estudiantes";
        }

        Matricula mat = estudianteService.obtenerMatriculaActual(id).orElse(null);

        model.addAttribute("estudiante", est);
        model.addAttribute("matriculaActual", mat);
        model.addAttribute("seccionSeleccionada", mat != null ? mat.getSeccion().getId() : null);
        model.addAttribute("turnoSeleccionado", mat != null ? mat.getTurno().getId() : null);
        model.addAttribute("secciones", seccionRepository.findAll());
        model.addAttribute("turnos", turnoRepository.findAll());
        model.addAttribute("esEdicion", true);
        return "admin/estudiantes/formulario";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("estudiante") Estudiante estudiante,
                             BindingResult bindingResult,
                             @RequestParam(value = "seccionId", required = false) Long seccionId,
                             @RequestParam(value = "turnoId", required = false) Long turnoId,
                             Model model,
                             RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("secciones", seccionRepository.findAll());
            model.addAttribute("turnos", turnoRepository.findAll());
            model.addAttribute("esEdicion", true);
            return "admin/estudiantes/formulario";
        }

        estudianteService.actualizarEstudiante(id, estudiante, seccionId, turnoId);
        flash.addFlashAttribute("success", "Datos del estudiante actualizados correctamente.");
        return "redirect:/admin/estudiantes";
    }

    @PostMapping("/{id}/toggle-estado")
    public String toggleEstado(@PathVariable("id") Long id, RedirectAttributes flash) {
        estudianteService.alternarEstado(id);
        flash.addFlashAttribute("success", "Estado del estudiante actualizado correctamente.");
        return "redirect:/admin/estudiantes";
    }

    @GetMapping("/{id}/fotocheck")
    public String fotocheck(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Estudiante est = estudianteService.obtenerPorId(id).orElse(null);
        if (est == null) {
            flash.addFlashAttribute("error", "Estudiante no encontrado.");
            return "redirect:/admin/estudiantes";
        }

        Matricula mat = estudianteService.obtenerMatriculaActual(id).orElse(null);

        model.addAttribute("estudiante", est);
        model.addAttribute("matricula", mat);
        return "admin/estudiantes/fotocheck";
    }

    @GetMapping("/importar")
    public String formularioImportar(Model model) {
        return "admin/estudiantes/importar";
    }

    @PostMapping("/importar")
    public String procesarImportacion(
            @RequestParam("archivoCsv") MultipartFile archivoCsv,
            Model model,
            RedirectAttributes flash) {

        if (archivoCsv == null || archivoCsv.isEmpty()) {
            flash.addFlashAttribute("error", "Por favor seleccione un archivo CSV válido.");
            return "redirect:/admin/estudiantes/importar";
        }

        try {
            ResultadoImportacion resultado = estudianteService.importarDesdeCsv(archivoCsv.getInputStream());
            model.addAttribute("resultado", resultado);
            return "admin/estudiantes/importar";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al procesar el archivo CSV: " + e.getMessage());
            return "redirect:/admin/estudiantes/importar";
        }
    }

    @GetMapping("/plantilla-csv")
    public ResponseEntity<byte[]> descargarPlantillaCsv() {
        String contenido = "DNI,APELLIDOS,NOMBRES,FECHA_NACIMIENTO,GENERO,NIVEL,GRADO,SECCION,TURNO,DNI_APODERADO\n"
                + "71234580,Perez Mendoza,Carlos David,2017-04-15,M,PRIMARIA,1o Grado de Primaria,A,MANANA,10234567\n"
                + "71234581,Gomez Ramirez,Camila Lucia,2016-09-22,F,PRIMARIA,2o Grado de Primaria,A,MANANA,10234568\n"
                + "71234582,Flores Diaz,Rodrigo Alexander,2013-02-14,M,SECUNDARIA,1o Ano de Secundaria,A,MANANA,10234569\n";

        byte[] bytes = contenido.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla_estudiantes_jeshua.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
