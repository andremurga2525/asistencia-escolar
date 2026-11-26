package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.model.EnumEstadoJustificacion;
import com.colegio.asistencia.model.Usuario;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.JustificacionInasistenciaRepository;
import com.colegio.asistencia.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/perfil")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracionColegioRepository configRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;

    public AdminPerfilController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            ConfiguracionColegioRepository configRepository,
            JustificacionInasistenciaRepository justificacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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
    public String verPerfil(Authentication auth, Model model, RedirectAttributes flash) {
        String usernameActual = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(usernameActual).orElse(null);

        if (usuario == null) {
            flash.addFlashAttribute("error", "No se encontró el usuario actual en el sistema.");
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("usuario", usuario);
        return "admin/perfil";
    }

    @PostMapping("/actualizar")
    public String actualizarPerfil(
            @RequestParam("nombreCompleto") String nombreCompleto,
            @RequestParam("email") String email,
            @RequestParam(value = "telefono", required = false) String telefono,
            @RequestParam("nuevoUsername") String nuevoUsername,
            @RequestParam(value = "nuevaPassword", required = false) String nuevaPassword,
            Authentication auth,
            RedirectAttributes flash) {

        String usernameActual = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(usernameActual)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String cleanUsername = nuevoUsername != null ? nuevoUsername.trim() : "";
        if (cleanUsername.isEmpty()) {
            flash.addFlashAttribute("error", "El nombre de usuario no puede estar vacío.");
            return "redirect:/admin/perfil";
        }

        // Si cambió de username, validar que no esté ocupado por otro usuario
        if (!cleanUsername.equalsIgnoreCase(usernameActual)) {
            if (usuarioRepository.existsByUsername(cleanUsername)) {
                flash.addFlashAttribute("error", "El nombre de usuario '" + cleanUsername + "' ya está en uso por otra persona. Elija otro.");
                return "redirect:/admin/perfil";
            }
            usuario.setUsername(cleanUsername);
        }

        usuario.setNombreCompleto(nombreCompleto != null ? nombreCompleto.trim() : usuario.getNombreCompleto());
        usuario.setEmail(email != null ? email.trim() : usuario.getEmail());
        usuario.setTelefono(telefono != null ? telefono.trim() : usuario.getTelefono());

        // Actualizar contraseña solo si se ingresó una nueva
        boolean cambioPassword = false;
        if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
            String cleanPass = nuevaPassword.trim();
            if (cleanPass.length() < 4) {
                flash.addFlashAttribute("error", "La nueva contraseña debe tener al menos 4 caracteres.");
                return "redirect:/admin/perfil";
            }
            usuario.setPassword(passwordEncoder.encode(cleanPass));
            cambioPassword = true;
        }

        usuarioRepository.save(usuario);

        // Si cambió el username, refrescar el contexto de Spring Security para no cerrar la sesión
        if (!cleanUsername.equalsIgnoreCase(usernameActual)) {
            Authentication nuevaAuth = new UsernamePasswordAuthenticationToken(cleanUsername, auth.getCredentials(), auth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(nuevaAuth);
        }

        String mensaje = "¡Perfil actualizado con éxito! " +
                (cambioPassword ? "Su nueva contraseña ha sido establecida. " : "") +
                "Su usuario de acceso es: " + usuario.getUsername();

        flash.addFlashAttribute("success", mensaje);
        return "redirect:/admin/perfil";
    }
}
