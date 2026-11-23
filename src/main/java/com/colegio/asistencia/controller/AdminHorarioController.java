package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/horarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHorarioController {

    private final HorarioEscolarRepository horarioRepository;
    private final NivelEducativoRepository nivelRepository;
    private final TurnoRepository turnoRepository;
    private final ConfiguracionColegioRepository configRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;

    public AdminHorarioController(
            HorarioEscolarRepository horarioRepository,
            NivelEducativoRepository nivelRepository,
            TurnoRepository turnoRepository,
            ConfiguracionColegioRepository configRepository,
            JustificacionInasistenciaRepository justificacionRepository) {
        this.horarioRepository = horarioRepository;
        this.nivelRepository = nivelRepository;
        this.turnoRepository = turnoRepository;
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
    public String listar(Model model) {
        model.addAttribute("horarios", horarioRepository.findAllByOrderByNivelIdAsc());
        return "admin/horarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevoFormulario(Model model) {
        HorarioEscolar nuevo = new HorarioEscolar();
        nuevo.setActivo(true);
        nuevo.setToleranciaMinutos(10);
        model.addAttribute("horario", nuevo);
        model.addAttribute("niveles", nivelRepository.findAll());
        model.addAttribute("turnos", turnoRepository.findAll());
        model.addAttribute("esEdicion", false);
        return "admin/horarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("horario") HorarioEscolar horario,
            BindingResult bindingResult,
            @RequestParam("nivelId") Long nivelId,
            @RequestParam("turnoId") Long turnoId,
            Model model,
            RedirectAttributes flash) {

        NivelEducativo nivel = nivelRepository.findById(nivelId).orElse(null);
        Turno turno = turnoRepository.findById(turnoId).orElse(null);

        if (nivel == null || turno == null) {
            flash.addFlashAttribute("error", "Debe seleccionar un nivel educativo y un turno válidos.");
            return "redirect:/admin/horarios/nuevo";
        }

        horario.setNivel(nivel);
        horario.setTurno(turno);

        if (bindingResult.hasErrors()) {
            model.addAttribute("niveles", nivelRepository.findAll());
            model.addAttribute("turnos", turnoRepository.findAll());
            model.addAttribute("esEdicion", false);
            return "admin/horarios/formulario";
        }

        horarioRepository.save(horario);
        flash.addFlashAttribute("success", "Horario escolar configurado exitosamente para " + nivel.getNombre() + " (" + turno.getNombre() + ").");
        return "redirect:/admin/horarios";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        HorarioEscolar h = horarioRepository.findById(id).orElse(null);
        if (h == null) {
            flash.addFlashAttribute("error", "Horario no encontrado.");
            return "redirect:/admin/horarios";
        }

        model.addAttribute("horario", h);
        model.addAttribute("niveles", nivelRepository.findAll());
        model.addAttribute("turnos", turnoRepository.findAll());
        model.addAttribute("esEdicion", true);
        return "admin/horarios/formulario";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("horario") HorarioEscolar horario,
            BindingResult bindingResult,
            @RequestParam("nivelId") Long nivelId,
            @RequestParam("turnoId") Long turnoId,
            Model model,
            RedirectAttributes flash) {

        HorarioEscolar existente = horarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));

        NivelEducativo nivel = nivelRepository.findById(nivelId).orElse(null);
        Turno turno = turnoRepository.findById(turnoId).orElse(null);

        if (nivel == null || turno == null) {
            flash.addFlashAttribute("error", "Nivel o turno inválido.");
            return "redirect:/admin/horarios/" + id + "/editar";
        }

        existente.setNombre(horario.getNombre());
        existente.setNivel(nivel);
        existente.setTurno(turno);
        existente.setHoraAperturaRegistro(horario.getHoraAperturaRegistro());
        existente.setHoraEntrada(horario.getHoraEntrada());
        existente.setToleranciaMinutos(horario.getToleranciaMinutos());
        existente.setHoraLimiteTardanza(horario.getHoraLimiteTardanza());
        existente.setHoraInicioSalida(horario.getHoraInicioSalida());
        existente.setHoraSalida(horario.getHoraSalida());
        existente.setActivo(horario.getActivo());

        horarioRepository.save(existente);
        flash.addFlashAttribute("success", "Horario escolar actualizado correctamente.");
        return "redirect:/admin/horarios";
    }

    @PostMapping("/{id}/toggle-estado")
    public String toggleEstado(@PathVariable("id") Long id, RedirectAttributes flash) {
        HorarioEscolar h = horarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));
        h.setActivo(!Boolean.TRUE.equals(h.getActivo()));
        horarioRepository.save(h);
        flash.addFlashAttribute("success", "Estado del horario escolar actualizado.");
        return "redirect:/admin/horarios";
    }
}
