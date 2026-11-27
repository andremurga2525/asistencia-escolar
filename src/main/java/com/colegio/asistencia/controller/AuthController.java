package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
                .orElseGet(() -> new ConfiguracionColegio(
                        "I.E.P. Internacional Jeshua",
                        "Instruye al niño en su camino, y aun cuando fuere viejo no se apartará de él",
                        "Tumán, Chiclayo, Lambayeque - Perú",
                        "+51 974 123 456",
                        "contacto@internacionaljeshua.edu.pe"
                ));

        model.addAttribute("colegio", colegio);

        if (error != null) {
            model.addAttribute("errorMessage", "Usuario o contraseña incorrectos. Por favor, verifique sus credenciales.");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "Ha cerrado sesión de forma segura.");
        }

        return "login";
    }

    /**
     * Endpoint público que sirve el logotipo oficial del colegio en alta resolución
     * para login, portales, escáner, fotochecks, reportes imprimibles y correos.
     */
    @GetMapping("/logo-colegio")
    @ResponseBody
    public ResponseEntity<byte[]> verLogoColegio() {
        Optional<ConfiguracionColegio> configOpt = configuracionRepository.findFirstByActivoTrue();
        if (configOpt.isPresent()) {
            ConfiguracionColegio config = configOpt.get();

            // 1. Prioridad: Imagen binaria guardada en MySQL (LONGBLOB) - persistente en la nube (Railway)
            if (config.getLogoData() != null && config.getLogoData().length > 0) {
                MediaType mediaType = MediaType.IMAGE_PNG;
                if (config.getLogoMimeType() != null && !config.getLogoMimeType().trim().isEmpty()) {
                    try {
                        mediaType = MediaType.parseMediaType(config.getLogoMimeType().trim());
                    } catch (Exception ignored) {}
                }
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                        .contentType(mediaType)
                        .body(config.getLogoData());
            }

            // 2. Si se configuró un enlace URL web externo (https://...)
            if (config.getLogoUrl() != null && (config.getLogoUrl().startsWith("http://") || config.getLogoUrl().startsWith("https://"))) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(config.getLogoUrl()))
                        .build();
            }
        }

        // 3. Fallback: buscar en disco local 'uploads/colegio'
        try {
            Path dir = Paths.get("uploads/colegio").toAbsolutePath().normalize();
            if (Files.exists(dir)) {
                try (var stream = Files.list(dir)) {
                    Optional<Path> archivo = stream.filter(Files::isRegularFile).findFirst();
                    if (archivo.isPresent()) {
                        byte[] bytes = Files.readAllBytes(archivo.get());
                        String probe = Files.probeContentType(archivo.get());
                        MediaType mt = probe != null ? MediaType.parseMediaType(probe) : MediaType.IMAGE_PNG;
                        return ResponseEntity.ok()
                                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                                .contentType(mt)
                                .body(bytes);
                    }
                }
            }
        } catch (Exception ignored) {}

        // 4. Fallback institucional: Escudo vectorial SVG representativo del Colegio Jeshua
        String defaultSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120' width='120' height='120'>"
                + "<defs>"
                + "  <linearGradient id='gradRed' x1='0%' y1='0%' x2='100%' y2='100%'>"
                + "    <stop offset='0%' stop-color='#b91c1c'/>"
                + "    <stop offset='100%' stop-color='#7f1d1d'/>"
                + "  </linearGradient>"
                + "</defs>"
                + "<path d='M60 5 C95 5, 110 25, 110 60 C110 95, 60 115, 60 115 C60 115, 10 95, 10 60 C10 25, 25 5, 60 5 Z' fill='url(#gradRed)' stroke='#facc15' stroke-width='4'/>"
                + "<path d='M60 22 L92 38 L60 54 L28 38 Z' fill='#facc15'/>"
                + "<path d='M38 46 L38 68 Q60 82 82 68 L82 46 L60 56 Z' fill='#ffffff'/>"
                + "<text x='60' y='75' font-family='Arial, sans-serif' font-weight='900' font-size='20' fill='#b91c1c' text-anchor='middle'>IJ</text>"
                + "</svg>";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(defaultSvg.getBytes(StandardCharsets.UTF_8));
    }
}
