package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.model.EnumEstadoJustificacion;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.JustificacionInasistenciaRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/admin/configuracion")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfiguracionController {

    private final ConfiguracionColegioRepository configRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;

    public AdminConfiguracionController(
            ConfiguracionColegioRepository configRepository,
            JustificacionInasistenciaRepository justificacionRepository) {
        this.configRepository = configRepository;
        this.justificacionRepository = justificacionRepository;
    }

    @ModelAttribute
    public void agregarConfiguracion(Model model) {
        model.addAttribute("totalJustificacionesPendientes", justificacionRepository.countByEstado(EnumEstadoJustificacion.PENDIENTE));
    }

    @GetMapping
    public String verConfiguracion(Model model) {
        ConfiguracionColegio config = configRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio(
                        "I.E.P. Internacional Jeshua",
                        "Instruye al nino en su camino, y aun cuando fuere viejo no se apartara de el",
                        "Tuman, Chiclayo, Lambayeque - Peru",
                        "+51 974 123 456",
                        "contacto@internacionaljeshua.edu.pe"
                ));

        model.addAttribute("configuracion", config);
        model.addAttribute("configColegio", config);
        return "admin/configuracion/colegio";
    }

    @PostMapping("/guardar")
    public String guardarConfiguracion(
            @Valid @ModelAttribute("configuracion") ConfiguracionColegio configForm,
            BindingResult bindingResult,
            @RequestParam(value = "archivoLogo", required = false) MultipartFile archivoLogo,
            @RequestParam(value = "quitarLogo", required = false, defaultValue = "false") boolean quitarLogo,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("configColegio", configForm);
            return "admin/configuracion/colegio";
        }

        ConfiguracionColegio existente = configRepository.findFirstByActivoTrue()
                .orElse(new ConfiguracionColegio());

        existente.setNombreColegio(configForm.getNombreColegio());
        existente.setLema(configForm.getLema());
        existente.setDireccion(configForm.getDireccion());
        existente.setTelefono(configForm.getTelefono());
        existente.setEmail(configForm.getEmail());
        existente.setColorPrimario(configForm.getColorPrimario());
        existente.setColorSecundario(configForm.getColorSecundario());

        // Manejar subida o eliminación de imagen de logotipo
        if (quitarLogo) {
            existente.setLogoUrl(null);
            try {
                Path dir = Paths.get("uploads/colegio").toAbsolutePath().normalize();
                if (Files.exists(dir)) {
                    try (var stream = Files.list(dir)) {
                        stream.forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
                    }
                }
            } catch (Exception ignored) {}
        } else if (archivoLogo != null && !archivoLogo.isEmpty()) {
            try {
                String originalName = archivoLogo.getOriginalFilename() != null ? archivoLogo.getOriginalFilename() : "logo.png";
                String ext = ".png";
                int dot = originalName.lastIndexOf('.');
                if (dot > 0) {
                    ext = originalName.substring(dot).toLowerCase();
                }

                Path dir = Paths.get("uploads/colegio").toAbsolutePath().normalize();
                Files.createDirectories(dir);

                // Limpiar logos anteriores para no acumular archivos
                try (var stream = Files.list(dir)) {
                    stream.forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
                }

                Path destino = dir.resolve("logo_institucional" + ext);
                Files.copy(archivoLogo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

                existente.setLogoUrl("/logo-colegio?v=" + System.currentTimeMillis());
            } catch (Exception e) {
                flash.addFlashAttribute("error", "Error al procesar la imagen del logotipo: " + e.getMessage());
                return "redirect:/admin/configuracion";
            }
        } else if (configForm.getLogoUrl() != null && !configForm.getLogoUrl().trim().isEmpty()) {
            existente.setLogoUrl(configForm.getLogoUrl().trim());
        }

        configRepository.save(existente);
        flash.addFlashAttribute("success", "Identidad institucional actualizada exitosamente.");
        return "redirect:/admin/configuracion";
    }
}
