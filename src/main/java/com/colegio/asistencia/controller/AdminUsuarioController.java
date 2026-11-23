package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracionColegioRepository configRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;

    public AdminUsuarioController(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            ConfiguracionColegioRepository configRepository,
            JustificacionInasistenciaRepository justificacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
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
    public String listar(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByNombreCompletoAsc();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("totalUsuarios", usuarios.size());
        return "admin/usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevoFormulario(Model model) {
        Usuario nuevo = new Usuario();
        nuevo.setActivo(true);
        model.addAttribute("usuario", nuevo);
        model.addAttribute("rolesDisponibles", rolRepository.findAll());
        model.addAttribute("esEdicion", false);
        return "admin/usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult bindingResult,
            @RequestParam(value = "rolesIds", required = false) List<Long> rolesIds,
            Model model,
            RedirectAttributes flash) {

        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            bindingResult.rejectValue("username", "error.usuario", "El nombre de usuario ya está en uso.");
        }
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            bindingResult.rejectValue("email", "error.usuario", "El correo electrónico ya está registrado.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("rolesDisponibles", rolRepository.findAll());
            model.addAttribute("esEdicion", false);
            return "admin/usuarios/formulario";
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        if (rolesIds != null && !rolesIds.isEmpty()) {
            Set<Rol> roles = new HashSet<>(rolRepository.findAllById(rolesIds));
            usuario.setRoles(roles);
        } else {
            // Rol por defecto si no se selecciona: AUXILIAR
            rolRepository.findByNombre(EnumRol.ROLE_AUXILIAR).ifPresent(usuario::agregarRol);
        }

        usuarioRepository.save(usuario);
        flash.addFlashAttribute("success", "Usuario '" + usuario.getUsername() + "' registrado exitosamente.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            flash.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/admin/usuarios";
        }

        model.addAttribute("usuario", u);
        model.addAttribute("rolesDisponibles", rolRepository.findAll());
        model.addAttribute("esEdicion", true);
        return "admin/usuarios/formulario";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable("id") Long id,
            @ModelAttribute("usuario") Usuario usuario,
            @RequestParam(value = "rolesIds", required = false) List<Long> rolesIds,
            RedirectAttributes flash) {

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        existente.setNombreCompleto(usuario.getNombreCompleto());
        existente.setEmail(usuario.getEmail());
        existente.setTelefono(usuario.getTelefono());
        if (usuario.getActivo() != null) {
            existente.setActivo(usuario.getActivo());
        }

        if (rolesIds != null && !rolesIds.isEmpty()) {
            Set<Rol> roles = new HashSet<>(rolRepository.findAllById(rolesIds));
            existente.setRoles(roles);
        }

        usuarioRepository.save(existente);
        flash.addFlashAttribute("success", "Usuario actualizado correctamente.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/toggle-estado")
    public String toggleEstado(@PathVariable("id") Long id, RedirectAttributes flash) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setActivo(!Boolean.TRUE.equals(u.getActivo()));
        usuarioRepository.save(u);
        flash.addFlashAttribute("success", "Estado del usuario actualizado.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable("id") Long id,
            @RequestParam("nuevaPassword") String nuevaPassword,
            RedirectAttributes flash) {
        if (nuevaPassword == null || nuevaPassword.trim().length() < 4) {
            flash.addFlashAttribute("error", "La contraseña debe tener al menos 4 caracteres.");
            return "redirect:/admin/usuarios/" + id + "/editar";
        }
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setPassword(passwordEncoder.encode(nuevaPassword.trim()));
        usuarioRepository.save(u);
        flash.addFlashAttribute("success", "Contraseña cambiada exitosamente para " + u.getUsername());
        return "redirect:/admin/usuarios/" + id + "/editar";
    }
}
