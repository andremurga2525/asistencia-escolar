package com.colegio.asistencia.service;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class EstudianteService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteService.class);

    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final SeccionRepository seccionRepository;
    private final TurnoRepository turnoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final GradoRepository gradoRepository;
    private final NivelEducativoRepository nivelRepository;

    public EstudianteService(
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            AnioEscolarRepository anioEscolarRepository,
            SeccionRepository seccionRepository,
            TurnoRepository turnoRepository,
            ApoderadoRepository apoderadoRepository,
            GradoRepository gradoRepository,
            NivelEducativoRepository nivelRepository) {
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.anioEscolarRepository = anioEscolarRepository;
        this.seccionRepository = seccionRepository;
        this.turnoRepository = turnoRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.gradoRepository = gradoRepository;
        this.nivelRepository = nivelRepository;
    }

    public List<Estudiante> listarTodos() {
        return estudianteRepository.findAllByOrderByApellidosAscNombresAsc();
    }

    public List<Estudiante> buscar(String criterio) {
        if (criterio == null || criterio.trim().isEmpty()) {
            return listarTodos();
        }
        return estudianteRepository.buscarPorTermino(criterio.trim());
    }

    public Optional<Estudiante> obtenerPorId(Long id) {
        return estudianteRepository.findById(id);
    }

    public Optional<Estudiante> obtenerPorDni(String dni) {
        return estudianteRepository.findByDni(dni);
    }

    public Optional<Estudiante> obtenerPorCodigoQr(String codigoQr) {
        return estudianteRepository.findByCodigoQr(codigoQr);
    }

    public boolean existeDni(String dni) {
        return estudianteRepository.existsByDni(dni);
    }

    public Optional<Matricula> obtenerMatriculaActual(Long estudianteId) {
        return matriculaRepository.findByEstudianteIdAndAnioEscolarActivoTrue(estudianteId);
    }

    @Transactional
    public Estudiante guardarNuevoEstudiante(Estudiante estudiante, Long seccionId, Long turnoId) {
        if (estudiante.getCodigoQr() == null || estudiante.getCodigoQr().trim().isEmpty()) {
            estudiante.setCodigoQr("JESHUA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (estudiante.getActivo() == null) {
            estudiante.setActivo(true);
        }

        Estudiante guardado = estudianteRepository.save(estudiante);

        // Matricular si se indicaron seccion y turno y hay un ano escolar activo
        if (seccionId != null && turnoId != null) {
            AnioEscolar anioActivo = anioEscolarRepository.findFirstByActivoTrue().orElse(null);
            Seccion seccion = seccionRepository.findById(seccionId).orElse(null);
            Turno turno = turnoRepository.findById(turnoId).orElse(null);

            if (anioActivo != null && seccion != null && turno != null) {
                String codMatricula = "MAT-" + anioActivo.getAnio() + "-" + String.format("%04d", guardado.getId());
                Matricula matricula = new Matricula(codMatricula, guardado, anioActivo, seccion, turno);
                matriculaRepository.save(matricula);
            }
        }

        return guardado;
    }

    @Transactional
    public Estudiante actualizarEstudiante(Long id, Estudiante datos, Long seccionId, Long turnoId) {
        Estudiante existente = estudianteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado con ID: " + id));

        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setFechaNacimiento(datos.getFechaNacimiento());
        existente.setGenero(datos.getGenero());
        existente.setDireccion(datos.getDireccion());
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }

        Estudiante actualizado = estudianteRepository.save(existente);

        // Actualizar o crear matricula
        if (seccionId != null && turnoId != null) {
            AnioEscolar anioActivo = anioEscolarRepository.findFirstByActivoTrue().orElse(null);
            Seccion seccion = seccionRepository.findById(seccionId).orElse(null);
            Turno turno = turnoRepository.findById(turnoId).orElse(null);

            if (anioActivo != null && seccion != null && turno != null) {
                Optional<Matricula> matOpt = matriculaRepository.findByEstudianteIdAndAnioEscolarActivoTrue(id);
                if (matOpt.isPresent()) {
                    Matricula mat = matOpt.get();
                    mat.setSeccion(seccion);
                    mat.setTurno(turno);
                    matriculaRepository.save(mat);
                } else {
                    String codMatricula = "MAT-" + anioActivo.getAnio() + "-" + String.format("%04d", id);
                    Matricula matricula = new Matricula(codMatricula, actualizado, anioActivo, seccion, turno);
                    matriculaRepository.save(matricula);
                }
            }
        }

        return actualizado;
    }

    @Transactional
    public void alternarEstado(Long id) {
        Estudiante est = estudianteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado con ID: " + id));
        est.setActivo(!Boolean.TRUE.equals(est.getActivo()));
        estudianteRepository.save(est);
    }

    /**
     * Importa masivamente estudiantes desde un archivo CSV.
     * Formato esperado: DNI, APELLIDOS, NOMBRES, FECHA_NACIMIENTO, GENERO, NIVEL, GRADO, SECCION, TURNO, DNI_APODERADO
     */
    @Transactional
    public ResultadoImportacion importarDesdeCsv(InputStream is) {
        ResultadoImportacion resultado = new ResultadoImportacion();
        AnioEscolar anioActivo = anioEscolarRepository.findFirstByActivoTrue().orElse(null);
        List<Seccion> todasLasSecciones = seccionRepository.findAll();
        Turno turnoDefecto = turnoRepository.findByCodigo(EnumTurno.MANANA).orElseGet(() -> turnoRepository.findAll().stream().findFirst().orElse(null));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String linea;
            int numLinea = 0;

            while ((linea = reader.readLine()) != null) {
                numLinea++;
                // Limpiar BOM UTF-8 si existe en la primera linea
                if (numLinea == 1 && linea.startsWith("\uFEFF")) {
                    linea = linea.substring(1);
                }

                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                // Detectar separador (coma o punto y coma)
                String separador = linea.contains(";") ? ";" : ",";
                String[] columnas = linea.split(separador, -1);

                // Omitir cabecera
                if (numLinea == 1 && (columnas[0].equalsIgnoreCase("dni") || columnas[0].toLowerCase().contains("documento"))) {
                    continue;
                }

                if (columnas.length < 3) {
                    resultado.agregarError("Línea " + numLinea + ": formato inválido (columnas insuficientes).");
                    continue;
                }

                resultado.incrementarProcesados();

                String dni = columnas[0].trim();
                String apellidos = columnas.length > 1 ? columnas[1].trim() : "";
                String nombres = columnas.length > 2 ? columnas[2].trim() : "";
                String fechaNacStr = columnas.length > 3 ? columnas[3].trim() : "";
                String genero = columnas.length > 4 ? columnas[4].trim().toUpperCase() : "M";
                String nivelStr = columnas.length > 5 ? columnas[5].trim() : "";
                String gradoStr = columnas.length > 6 ? columnas[6].trim() : "";
                String seccionStr = columnas.length > 7 ? columnas[7].trim() : "A";
                String turnoStr = columnas.length > 8 ? columnas[8].trim() : "MANANA";
                String dniApoderado = columnas.length > 9 ? columnas[9].trim() : "";

                // Validar DNI
                if (!dni.matches("^[0-9]{8}$")) {
                    resultado.agregarError("Línea " + numLinea + ": DNI '" + dni + "' no válido (debe tener 8 dígitos).");
                    continue;
                }

                if (nombres.isEmpty() || apellidos.isEmpty()) {
                    resultado.agregarError("Línea " + numLinea + ": Nombres y Apellidos son obligatorios.");
                    continue;
                }

                LocalDate fechaNacimiento = parsearFecha(fechaNacStr);
                if (!genero.equals("M") && !genero.equals("F")) {
                    genero = "M";
                }

                // Crear o actualizar Estudiante
                Optional<Estudiante> estudianteOpt = estudianteRepository.findByDni(dni);
                Estudiante estudiante;
                boolean esNuevo = false;

                if (estudianteOpt.isPresent()) {
                    estudiante = estudianteOpt.get();
                    estudiante.setNombres(nombres);
                    estudiante.setApellidos(apellidos);
                    estudiante.setFechaNacimiento(fechaNacimiento);
                    estudiante.setGenero(genero);
                    estudiante = estudianteRepository.save(estudiante);
                    resultado.incrementarActualizados();
                } else {
                    esNuevo = true;
                    estudiante = new Estudiante(dni, nombres, apellidos, genero);
                    estudiante.setFechaNacimiento(fechaNacimiento);
                    estudiante.setCodigoQr("JESHUA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    estudiante.setActivo(true);
                    estudiante = estudianteRepository.save(estudiante);
                    resultado.incrementarCreados();
                }

                // Matrícula automática si hay año activo
                if (anioActivo != null) {
                    Seccion seccionEncontrada = resolverSeccion(todasLasSecciones, nivelStr, gradoStr, seccionStr);
                    Turno turnoEncontrado = resolverTurno(turnoStr, turnoDefecto);

                    if (seccionEncontrada != null) {
                        Optional<Matricula> matOpt = matriculaRepository.findByEstudianteIdAndAnioEscolarActivoTrue(estudiante.getId());
                        if (matOpt.isEmpty()) {
                            String codMatricula = "MAT-" + anioActivo.getAnio() + "-" + String.format("%04d", estudiante.getId());
                            Matricula matricula = new Matricula(codMatricula, estudiante, anioActivo, seccionEncontrada, turnoEncontrado);
                            matriculaRepository.save(matricula);
                            resultado.incrementarMatriculados();
                        }
                    }
                }

                // Vincular apoderado si se indicó su DNI
                if (dniApoderado.matches("^[0-9]{8}$")) {
                    Optional<Apoderado> apoOpt = apoderadoRepository.findByDni(dniApoderado);
                    if (apoOpt.isPresent()) {
                        Apoderado apo = apoOpt.get();
                        apo.agregarEstudiante(estudiante);
                        apoderadoRepository.save(apo);
                        resultado.incrementarApoderadosVinculados();
                    }
                }

                resultado.agregarExitoso("Estudiante " + (esNuevo ? "creado" : "actualizado") + ": " + apellidos + ", " + nombres + " (" + dni + ")");
            }

        } catch (Exception e) {
            log.error("Error al procesar archivo CSV de estudiantes", e);
            resultado.agregarError("Error crítico al procesar archivo: " + e.getMessage());
        }

        return resultado;
    }

    private LocalDate parsearFecha(String str) {
        if (str == null || str.trim().isEmpty()) {
            return LocalDate.of(2015, 1, 1);
        }
        String limpia = str.trim();
        try {
            if (limpia.contains("/")) {
                String[] p = limpia.split("/");
                if (p.length == 3) {
                    return LocalDate.of(Integer.parseInt(p[2].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[0].trim()));
                }
            } else if (limpia.contains("-")) {
                String[] p = limpia.split("-");
                if (p.length == 3) {
                    if (p[0].length() == 4) {
                        return LocalDate.of(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
                    } else {
                        return LocalDate.of(Integer.parseInt(p[2].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[0].trim()));
                    }
                }
            }
        } catch (Exception ignored) {}
        return LocalDate.of(2015, 1, 1);
    }

    private Seccion resolverSeccion(List<Seccion> secciones, String nivelStr, String gradoStr, String seccionStr) {
        if (secciones.isEmpty()) return null;

        // Intentar coincidencia exacta o parcial de grado y sección
        for (Seccion s : secciones) {
            boolean coincideSeccion = seccionStr == null || seccionStr.isEmpty() || s.getNombre().equalsIgnoreCase(seccionStr);
            if (coincideSeccion && gradoStr != null && !gradoStr.isEmpty()) {
                String nombreGrado = s.getGrado().getNombre().toLowerCase();
                String busqGrado = gradoStr.toLowerCase();
                if (nombreGrado.contains(busqGrado) || busqGrado.contains(s.getGrado().getNumero().toString())) {
                    if (nivelStr != null && !nivelStr.isEmpty()) {
                        if (s.getGrado().getNivel().getCodigo().name().equalsIgnoreCase(nivelStr)) {
                            return s;
                        }
                    } else {
                        return s;
                    }
                }
            }
        }

        // Si no se encuentra exacta, devolver la primera sección disponible
        return secciones.get(0);
    }

    private Turno resolverTurno(String turnoStr, Turno turnoDefecto) {
        if (turnoStr != null) {
            String limpio = turnoStr.trim().toUpperCase();
            if (limpio.contains("TARDE")) {
                return turnoRepository.findByCodigo(EnumTurno.TARDE).orElse(turnoDefecto);
            }
        }
        return turnoDefecto;
    }
}
