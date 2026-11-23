package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
public class AuthController {

    private final ConfiguracionColegioRepository configuracionRepository;

    public AuthController(ConfiguracionColegioRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        // Si ya está autenticado, redirigir al inicio/dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        ConfiguracionColegio colegio = configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("Sistema Escolar", "Lema", "-", "-", "-"));

        model.addAttribute("colegio", colegio);

        if (error != null) {
            model.addAttribute("errorMessage", "Usuario o contraseña incorrectos. Por favor, verifique sus credenciales.");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "Ha cerrado sesión de forma segura.");
        }

        return "login";
    }

    @GetMapping("/logo-colegio")
    @ResponseBody
    public ResponseEntity<Resource> verLogoColegio() {
        try {
            Path dir = Paths.get("uploads/colegio").toAbsolutePath().normalize();
            if (Files.exists(dir)) {
                try (var stream = Files.list(dir)) {
                    Optional<Path> archivoLogo = stream
                            .filter(p -> p.getFileName().toString().startsWith("logo_institucional"))
                            .findFirst();
                    if (archivoLogo.isPresent()) {
                        Resource recurso = new UrlResource(archivoLogo.get().toUri());
                        String contentType = Files.probeContentType(archivoLogo.get());
                        if (contentType == null) contentType = "image/png";
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                                .body(recurso);
                    }
                }
            }
        } catch (Exception ignored) {}
        return ResponseEntity.notFound().build();
    }
}
