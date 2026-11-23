package com.colegio.asistencia.service;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ApoderadoService {

    private final ApoderadoRepository apoderadoRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public ApoderadoService(
            ApoderadoRepository apoderadoRepository,
            EstudianteRepository estudianteRepository,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.apoderadoRepository = apoderadoRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Apoderado> listarTodos() {
        return apoderadoRepository.findAllByOrderByApellidosAscNombresAsc();
    }

    public List<Apoderado> buscar(String criterio) {
        if (criterio == null || criterio.trim().isEmpty()) {
            return listarTodos();
        }
        return apoderadoRepository.buscarPorTermino(criterio.trim());
    }

    public Optional<Apoderado> obtenerPorId(Long id) {
        return apoderadoRepository.findById(id);
    }

    public Optional<Apoderado> obtenerPorDni(String dni) {
        return apoderadoRepository.findByDni(dni);
    }

    public boolean existeDni(String dni) {
        return apoderadoRepository.existsByDni(dni);
    }

    @Transactional
    public Apoderado registrarNuevoApoderado(Apoderado apoderado, List<Long> estudiantesIds, String passwordPersonalizada) {
        String dni = apoderado.getDni().trim();

        // 1. Crear o recuperar la cuenta de usuario para login del padre
        Usuario usuario;
        if (!usuarioRepository.existsByUsername(dni)) {
            String passRaw = (passwordPersonalizada != null && !passwordPersonalizada.trim().isEmpty())
                    ? passwordPersonalizada.trim()
                    : "padre123";

            usuario = new Usuario(
                    dni,
                    passwordEncoder.encode(passRaw),
                    apoderado.getNombreCompleto() + " (" + (apoderado.getParentesco() != null ? apoderado.getParentesco() : "Padre") + ")",
                    apoderado.getEmail() != null && !apoderado.getEmail().trim().isEmpty() ? apoderado.getEmail().trim() : "padre." + dni + "@internacionaljeshua.edu.pe"
            );
            usuario.setTelefono(apoderado.getTelefono());

            Rol rolPadre = rolRepository.findByNombre(EnumRol.ROLE_PADRE)
                    .orElseGet(() -> rolRepository.save(new Rol(EnumRol.ROLE_PADRE, "Padre o Apoderado")));
            usuario.agregarRol(rolPadre);
            usuario = usuarioRepository.save(usuario);
        } else {
            usuario = usuarioRepository.findByUsername(dni).orElse(null);
        }

        apoderado.setUsuario(usuario);
        if (apoderado.getActivo() == null) {
            apoderado.setActivo(true);
        }

        // 2. Vincular los estudiantes indicados
        if (estudiantesIds != null && !estudiantesIds.isEmpty()) {
            Set<Estudiante> estudiantes = new HashSet<>(estudianteRepository.findAllById(estudiantesIds));
            apoderado.setEstudiantes(estudiantes);
        }

        return apoderadoRepository.save(apoderado);
    }

    @Transactional
    public Apoderado actualizarApoderado(Long id, Apoderado datos, List<Long> estudiantesIds) {
        Apoderado existente = apoderadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Apoderado no encontrado con ID: " + id));

        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setParentesco(datos.getParentesco());
        existente.setTelefono(datos.getTelefono());
        existente.setEmail(datos.getEmail());
        existente.setDireccion(datos.getDireccion());
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }

        // Actualizar datos del usuario asociado
        if (existente.getUsuario() != null) {
            Usuario u = existente.getUsuario();
            u.setNombreCompleto(existente.getNombreCompleto() + " (" + existente.getParentesco() + ")");
            u.setTelefono(existente.getTelefono());
            if (existente.getEmail() != null && !existente.getEmail().trim().isEmpty()) {
                u.setEmail(existente.getEmail().trim());
            }
            usuarioRepository.save(u);
        }

        // Actualizar estudiantes vinculados
        if (estudiantesIds != null) {
            Set<Estudiante> estudiantes = new HashSet<>(estudianteRepository.findAllById(estudiantesIds));
            existente.setEstudiantes(estudiantes);
        }

        return apoderadoRepository.save(existente);
    }

    @Transactional
    public void vincularHijoPorDni(Long apoderadoId, String dniHijo) {
        Apoderado apoderado = apoderadoRepository.findById(apoderadoId)
                .orElseThrow(() -> new IllegalArgumentException("Apoderado no encontrado"));
        Estudiante hijo = estudianteRepository.findByDni(dniHijo.trim())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún estudiante con DNI: " + dniHijo));

        apoderado.agregarEstudiante(hijo);
        apoderadoRepository.save(apoderado);
    }

    @Transactional
    public void desvincularHijo(Long apoderadoId, Long estudianteId) {
        Apoderado apoderado = apoderadoRepository.findById(apoderadoId)
                .orElseThrow(() -> new IllegalArgumentException("Apoderado no encontrado"));
        Estudiante hijo = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        apoderado.removerEstudiante(hijo);
        apoderadoRepository.save(apoderado);
    }

    @Transactional
    public void alternarEstado(Long id) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Apoderado no encontrado"));
        boolean nuevoEstado = !Boolean.TRUE.equals(apoderado.getActivo());
        apoderado.setActivo(nuevoEstado);
        if (apoderado.getUsuario() != null) {
            apoderado.getUsuario().setActivo(nuevoEstado);
            usuarioRepository.save(apoderado.getUsuario());
        }
        apoderadoRepository.save(apoderado);
    }

    @Transactional
    public void resetearPassword(Long id, String nuevaPassword) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Apoderado no encontrado"));
        if (apoderado.getUsuario() != null) {
            Usuario u = apoderado.getUsuario();
            u.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioRepository.save(u);
        }
    }
}
