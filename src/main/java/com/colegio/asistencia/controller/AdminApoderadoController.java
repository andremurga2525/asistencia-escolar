package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.Apoderado;
import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.model.Estudiante;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.EstudianteRepository;
import com.colegio.asistencia.repository.JustificacionInasistenciaRepository;
import com.colegio.asistencia.model.EnumEstadoJustificacion;
import com.colegio.asistencia.service.ApoderadoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/apoderados")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApoderadoController {

    private final ApoderadoService apoderadoService;
    private final EstudianteRepository estudianteRepository;
    private final ConfiguracionColegioRepository configRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;

    public AdminApoderadoController(
            ApoderadoService apoderadoService,
            EstudianteRepository estudianteRepository,
            ConfiguracionColegioRepository configRepository,
            JustificacionInasistenciaRepository justificacionRepository) {
        this.apoderadoService = apoderadoService;
        this.estudianteRepository = estudianteRepository;
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
    public String listar(@RequestParam(value = "q", required = false) String busqueda, Model model) {
        List<Apoderado> apoderados = apoderadoService.buscar(busqueda);
        model.addAttribute("apoderados", apoderados);
        model.addAttribute("busqueda", busqueda != null ? busqueda : "");
        model.addAttribute("totalApoderados", apoderados.size());
        return "admin/apoderados/lista";
    }

    @GetMapping("/nuevo")
    public String nuevoFormulario(Model model) {
        Apoderado nuevo = new Apoderado();
        nuevo.setActivo(true);
        nuevo.setParentesco("PADRE");
        model.addAttribute("apoderado", nuevo);
        model.addAttribute("todosEstudiantes", estudianteRepository.findAllByOrderByApellidosAscNombresAsc());
        model.addAttribute("esEdicion", false);
        return "admin/apoderados/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("apoderado") Apoderado apoderado,
            BindingResult bindingResult,
            @RequestParam(value = "estudiantesIds", required = false) List<Long> estudiantesIds,
            @RequestParam(value = "passwordInicial", required = false, defaultValue = "padre123") String passwordInicial,
            Model model,
            RedirectAttributes flash) {

        if (apoderado.getId() == null && apoderadoService.existeDni(apoderado.getDni())) {
            bindingResult.rejectValue("dni", "error.apoderado", "Ya existe un apoderado registrado con este DNI.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("todosEstudiantes", estudianteRepository.findAllByOrderByApellidosAscNombresAsc());
            model.addAttribute("esEdicion", false);
            return "admin/apoderados/formulario";
        }

        apoderadoService.registrarNuevoApoderado(apoderado, estudiantesIds, passwordInicial);
        flash.addFlashAttribute("success", "Apoderado '" + apoderado.getNombreCompleto() + "' registrado exitosamente. Usuario de acceso creado con DNI: " + apoderado.getDni());
        return "redirect:/admin/apoderados";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Apoderado ap = apoderadoService.obtenerPorId(id).orElse(null);
        if (ap == null) {
            flash.addFlashAttribute("error", "Apoderado no encontrado.");
            return "redirect:/admin/apoderados";
        }

        model.addAttribute("apoderado", ap);
        model.addAttribute("todosEstudiantes", estudianteRepository.findAllByOrderByApellidosAscNombresAsc());
        model.addAttribute("esEdicion", true);
        return "admin/apoderados/formulario";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("apoderado") Apoderado apoderado,
            BindingResult bindingResult,
            @RequestParam(value = "estudiantesIds", required = false) List<Long> estudiantesIds,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("todosEstudiantes", estudianteRepository.findAllByOrderByApellidosAscNombresAsc());
            model.addAttribute("esEdicion", true);
            return "admin/apoderados/formulario";
        }

        apoderadoService.actualizarApoderado(id, apoderado, estudiantesIds);
        flash.addFlashAttribute("success", "Datos del apoderado actualizados correctamente.");
        return "redirect:/admin/apoderados";
    }

    @PostMapping("/{id}/vincular-estudiante")
    public String vincularEstudiante(
            @PathVariable("id") Long id,
            @RequestParam("dniHijo") String dniHijo,
            RedirectAttributes flash) {
        try {
            apoderadoService.vincularHijoPorDni(id, dniHijo);
            flash.addFlashAttribute("success", "Estudiante con DNI " + dniHijo + " vinculado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/apoderados/" + id + "/editar";
    }

    @PostMapping("/{id}/desvincular-estudiante/{estudianteId}")
    public String desvincularEstudiante(
            @PathVariable("id") Long id,
            @PathVariable("estudianteId") Long estudianteId,
            RedirectAttributes flash) {
        try {
            apoderadoService.desvincularHijo(id, estudianteId);
            flash.addFlashAttribute("success", "Estudiante desvinculado del apoderado.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/apoderados/" + id + "/editar";
    }

    @PostMapping("/{id}/toggle-estado")
    public String toggleEstado(@PathVariable("id") Long id, RedirectAttributes flash) {
        apoderadoService.alternarEstado(id);
        flash.addFlashAttribute("success", "Estado del apoderado y su acceso actualizado.");
        return "redirect:/admin/apoderados";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable("id") Long id,
            @RequestParam("nuevaPassword") String nuevaPassword,
            RedirectAttributes flash) {
        if (nuevaPassword == null || nuevaPassword.trim().length() < 4) {
            flash.addFlashAttribute("error", "La contraseña debe tener al menos 4 caracteres.");
            return "redirect:/admin/apoderados/" + id + "/editar";
        }
        apoderadoService.resetearPassword(id, nuevaPassword.trim());
        flash.addFlashAttribute("success", "Contraseña del portal de apoderados actualizada correctamente.");
        return "redirect:/admin/apoderados/" + id + "/editar";
    }
}
