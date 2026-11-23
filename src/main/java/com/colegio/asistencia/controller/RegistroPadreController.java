package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class RegistroPadreController {

    private static final Logger log = LoggerFactory.getLogger(RegistroPadreController.class);

    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracionColegioRepository configRepository;

    public RegistroPadreController(
            ApoderadoRepository apoderadoRepository,
            UsuarioRepository usuarioRepository,
            EstudianteRepository estudianteRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            ConfiguracionColegioRepository configRepository) {
        this.apoderadoRepository = apoderadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.configRepository = configRepository;
    }

    @GetMapping("/registro-padre")
    public String mostrarFormularioRegistro(Model model) {
        ConfiguracionColegio colegio = configRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio(
                        "I.E.P. Internacional Jeshua",
                        "Instruye al nino en su camino",
                        "Tuman, Chiclayo, Lambayeque - Peru",
                        "+51 974 123 456",
                        "contacto@internacionaljeshua.edu.pe"
                ));
        model.addAttribute("colegio", colegio);
        return "registro_padre";
    }

    @PostMapping("/registro-padre")
    public String procesarRegistro(
            @RequestParam("dni") String dni,
            @RequestParam("nombres") String nombres,
            @RequestParam("apellidos") String apellidos,
            @RequestParam(value = "parentesco", defaultValue = "PADRE") String parentesco,
            @RequestParam(value = "telefono", required = false) String telefono,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "direccion", required = false) String direccion,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "dniHijo", required = false) String dniHijo,
            Model model,
            RedirectAttributes flash) {

        ConfiguracionColegio colegio = configRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("I.E.P. Internacional Jeshua", "Lema", "-", "-", "-"));
        model.addAttribute("colegio", colegio);

        // Mantener valores ingresados en caso de error
        model.addAttribute("dni", dni);
        model.addAttribute("nombres", nombres);
        model.addAttribute("apellidos", apellidos);
        model.addAttribute("parentesco", parentesco);
        model.addAttribute("telefono", telefono);
        model.addAttribute("email", email);
        model.addAttribute("direccion", direccion);
        model.addAttribute("dniHijo", dniHijo);

        // 1. Validaciones de DNI
        String dniLimpio = dni != null ? dni.trim() : "";
        if (!dniLimpio.matches("^[0-9]{8}$")) {
            model.addAttribute("error", "El DNI debe contener exactamente 8 dígitos numéricos.");
            return "registro_padre";
        }

        if (apoderadoRepository.existsByDni(dniLimpio) || usuarioRepository.existsByUsername(dniLimpio)) {
            model.addAttribute("error", "El DNI " + dniLimpio + " ya se encuentra registrado en el sistema. Puede iniciar sesión directamente.");
            return "registro_padre";
        }

        // 2. Validación de Contraseña
        if (password == null || password.trim().length() < 4) {
            model.addAttribute("error", "La contraseña debe tener al menos 4 caracteres (se sugiere usar su fecha de nacimiento, ej: 15081985).");
            return "registro_padre";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden. Por favor, verifíquelas.");
            return "registro_padre";
        }

        // 3. Crear el Usuario para inicio de sesión (username = DNI)
        Rol rolPadre = rolRepository.findByNombre(EnumRol.ROLE_PADRE)
                .orElseGet(() -> rolRepository.save(new Rol(EnumRol.ROLE_PADRE, "Padre o Apoderado")));

        String emailFinal = (email != null && !email.trim().isEmpty())
                ? email.trim()
                : "padre." + dniLimpio + "@internacionaljeshua.edu.pe";

        Usuario usuario = new Usuario(
                dniLimpio,
                passwordEncoder.encode(password.trim()),
                apellidos.trim() + ", " + nombres.trim() + " (" + parentesco + ")",
                emailFinal
        );
        usuario.setTelefono(telefono != null ? telefono.trim() : null);
        usuario.agregarRol(rolPadre);
        usuario = usuarioRepository.save(usuario);

        // 4. Crear la entidad Apoderado
        Apoderado apoderado = new Apoderado(
                dniLimpio,
                nombres.trim(),
                apellidos.trim(),
                parentesco,
                telefono != null ? telefono.trim() : null
        );
        apoderado.setEmail(emailFinal);
        apoderado.setDireccion(direccion != null ? direccion.trim() : null);
        apoderado.setUsuario(usuario);
        apoderado.setActivo(true);

        // 5. Vincular hijo(a) si se proporcionó su DNI
        String mensajeHijo = "";
        if (dniHijo != null && !dniHijo.trim().isEmpty()) {
            String dniHijoLimpio = dniHijo.trim();
            Optional<Estudiante> estudianteOpt = estudianteRepository.findByDni(dniHijoLimpio);
            if (estudianteOpt.isPresent()) {
                apoderado.agregarEstudiante(estudianteOpt.get());
                mensajeHijo = " Se vinculó correctamente con su hijo(a): " + estudianteOpt.get().getNombreCompleto() + ".";
            } else {
                mensajeHijo = " Aviso: El DNI de estudiante (" + dniHijoLimpio + ") no fue hallado aún en la lista escolar; el director podrá vincularlo posteriormente.";
            }
        }

        apoderadoRepository.save(apoderado);
        log.info("Nuevo padre registrado: DNI={}, Nombre={} {}", dniLimpio, nombres, apellidos);

        flash.addFlashAttribute("successRegistro", "¡Cuenta creada exitosamente! Ya puede iniciar sesión con su DNI (" + dniLimpio + ") y su contraseña." + mensajeHijo);
        return "redirect:/login";
    }
}
