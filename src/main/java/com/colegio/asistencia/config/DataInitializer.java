package com.colegio.asistencia.config;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import com.colegio.asistencia.util.FechaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            JdbcTemplate jdbcTemplate,
            ConfiguracionColegioRepository configuracionRepository,
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AnioEscolarRepository anioEscolarRepository,
            NivelEducativoRepository nivelRepository,
            TurnoRepository turnoRepository,
            GradoRepository gradoRepository,
            SeccionRepository seccionRepository,
            HorarioEscolarRepository horarioRepository,
            EstudianteRepository estudianteRepository,
            ApoderadoRepository apoderadoRepository,
            MatriculaRepository matriculaRepository,
            RegistroAsistenciaRepository asistenciaRepository,
            JustificacionInasistenciaRepository justificacionRepository,
            TareaEscolarRepository tareaRepository,
            ComunicadoAulaRepository comunicadoRepository,
            ReporteConductaRepository reporteConductaRepository) {
        return args -> {
            // Asegurar que las columnas en MySQL permitan valores ampliados (evita Data truncation)
            try {
                jdbcTemplate.execute("ALTER TABLE configuracion_colegio MODIFY COLUMN color_primario VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE configuracion_colegio MODIFY COLUMN color_secundario VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE configuracion_colegio MODIFY COLUMN logo_url TEXT");
            } catch (Exception ignored) {}

            // 1. Configuracion Institucional: I.E.P. Internacional Jeshua (Tuman)
            if (configuracionRepository.count() == 0) {
                log.info("Inicializando configuracion institucional de I.E.P. Internacional Jeshua...");
                ConfiguracionColegio jeshua = new ConfiguracionColegio(
                        "I.E.P. Internacional Jeshua",
                        "Instruye al nino en su camino, y aun cuando fuere viejo no se apartara de el",
                        "Tuman, Chiclayo, Lambayeque - Peru",
                        "+51 974 123 456",
                        "contacto@internacionaljeshua.edu.pe"
                );
                jeshua.setColorPrimario("#b91c1c");
                jeshua.setColorSecundario("#ffffff");
                configuracionRepository.save(jeshua);
            } else {
                configuracionRepository.findFirstByActivoTrue().ifPresent(cfg -> {
                    if (cfg.getLogoUrl() != null && cfg.getLogoUrl().toLowerCase().contains("facebook")) {
                        cfg.setLogoUrl(null);
                        configuracionRepository.save(cfg);
                    }
                });
            }

            // 2. Roles del Sistema Escolar
            for (EnumRol enumRol : EnumRol.values()) {
                if (rolRepository.findByNombre(enumRol).isEmpty()) {
                    Rol rol = new Rol(enumRol, enumRol.getDescripcion());
                    rolRepository.save(rol);
                }
            }

            // 3. Usuarios Principales (Admin, Auxiliar, Profesor y Tutor)
            Rol rolAdmin = rolRepository.findByNombre(EnumRol.ROLE_ADMIN).orElse(null);
            Rol rolAuxiliar = rolRepository.findByNombre(EnumRol.ROLE_AUXILIAR).orElse(null);
            Rol rolPadre = rolRepository.findByNombre(EnumRol.ROLE_PADRE).orElse(null);
            Rol rolProfesor = rolRepository.findByNombre(EnumRol.ROLE_PROFESOR).orElse(null);
            Rol rolTutor = rolRepository.findByNombre(EnumRol.ROLE_TUTOR).orElse(null);

            Usuario adminUser = null;
            if (!usuarioRepository.existsByUsername("admin") && rolAdmin != null) {
                adminUser = new Usuario("admin", passwordEncoder.encode("admin123"), "Director General Jeshua", "admin@internacionaljeshua.edu.pe");
                adminUser.setTelefono("+51 974 111 222");
                adminUser.agregarRol(rolAdmin);
                adminUser = usuarioRepository.save(adminUser);
            } else {
                adminUser = usuarioRepository.findByUsername("admin").orElse(null);
            }

            Usuario auxiliarUser = null;
            if (!usuarioRepository.existsByUsername("auxiliar") && rolAuxiliar != null) {
                auxiliarUser = new Usuario("auxiliar", passwordEncoder.encode("auxiliar123"), "Carlos Mendoza (Auxiliar)", "auxiliar@internacionaljeshua.edu.pe");
                auxiliarUser.setTelefono("+51 974 333 444");
                auxiliarUser.agregarRol(rolAuxiliar);
                auxiliarUser = usuarioRepository.save(auxiliarUser);
            } else {
                auxiliarUser = usuarioRepository.findByUsername("auxiliar").orElse(null);
            }

            Usuario profesorUser = null;
            if (!usuarioRepository.existsByUsername("profesor") && rolProfesor != null) {
                profesorUser = new Usuario("profesor", passwordEncoder.encode("profesor123"), "Prof. Roberto Morales (Docente)", "profesor@internacionaljeshua.edu.pe");
                profesorUser.setTelefono("+51 974 555 777");
                profesorUser.agregarRol(rolProfesor);
                profesorUser = usuarioRepository.save(profesorUser);
            } else {
                profesorUser = usuarioRepository.findByUsername("profesor").orElse(null);
            }

            Usuario tutorUser = null;
            if (!usuarioRepository.existsByUsername("tutor") && rolTutor != null) {
                tutorUser = new Usuario("tutor", passwordEncoder.encode("tutor123"), "Lic. Carmen Delgado (Tutora 1° Primaria)", "tutor@internacionaljeshua.edu.pe");
                tutorUser.setTelefono("+51 974 777 999");
                tutorUser.agregarRol(rolTutor);
                if (rolProfesor != null) tutorUser.agregarRol(rolProfesor);
                tutorUser = usuarioRepository.save(tutorUser);
            } else {
                tutorUser = usuarioRepository.findByUsername("tutor").orElse(null);
            }

            // 4. Ano Escolar Oficial
            AnioEscolar anio2026;
            if (anioEscolarRepository.findByAnio(2026).isEmpty()) {
                anio2026 = anioEscolarRepository.save(new AnioEscolar(2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20), true));
            } else {
                anio2026 = anioEscolarRepository.findByAnio(2026).get();
            }

            // 5. Niveles Educativos
            for (EnumNivel enumNivel : EnumNivel.values()) {
                if (nivelRepository.findByCodigo(enumNivel).isEmpty()) {
                    nivelRepository.save(new NivelEducativo(enumNivel, enumNivel.getDescripcion()));
                }
            }

            // 6. Turnos
            for (EnumTurno enumTurno : EnumTurno.values()) {
                if (turnoRepository.findByCodigo(enumTurno).isEmpty()) {
                    turnoRepository.save(new Turno(enumTurno, enumTurno.getDescripcion()));
                }
            }

            // 7. Grados y Secciones
            NivelEducativo nivelPrimaria = nivelRepository.findByCodigo(EnumNivel.PRIMARIA).orElse(null);
            NivelEducativo nivelSecundaria = nivelRepository.findByCodigo(EnumNivel.SECUNDARIA).orElse(null);
            NivelEducativo nivelInicial = nivelRepository.findByCodigo(EnumNivel.INICIAL).orElse(null);

            if (gradoRepository.count() == 0 && nivelPrimaria != null && nivelSecundaria != null) {
                if (nivelInicial != null) {
                    crearGradoYSeccion(gradoRepository, seccionRepository, "Inicial 3 Anos", 3, nivelInicial, "A");
                    crearGradoYSeccion(gradoRepository, seccionRepository, "Inicial 4 Anos", 4, nivelInicial, "A");
                    crearGradoYSeccion(gradoRepository, seccionRepository, "Inicial 5 Anos", 5, nivelInicial, "A");
                }
                for (int i = 1; i <= 6; i++) {
                    crearGradoYSeccion(gradoRepository, seccionRepository, i + "o Grado de Primaria", i, nivelPrimaria, "A");
                }
                for (int i = 1; i <= 5; i++) {
                    crearGradoYSeccion(gradoRepository, seccionRepository, i + "o Ano de Secundaria", i, nivelSecundaria, "A");
                }
            }

            // 8. Horarios Escolares
            Turno turnoManana = turnoRepository.findByCodigo(EnumTurno.MANANA).orElse(null);
            if (horarioRepository.count() == 0 && turnoManana != null && nivelPrimaria != null && nivelSecundaria != null) {
                if (nivelInicial != null) {
                    horarioRepository.save(new HorarioEscolar("Horario Inicial Manana", nivelInicial, turnoManana,
                            LocalTime.of(7, 30), LocalTime.of(8, 0), 10, LocalTime.of(8, 30), LocalTime.of(12, 30), LocalTime.of(13, 0)));
                }
                horarioRepository.save(new HorarioEscolar("Horario Primaria Manana", nivelPrimaria, turnoManana,
                        LocalTime.of(7, 0), LocalTime.of(7, 30), 10, LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(13, 30)));
                horarioRepository.save(new HorarioEscolar("Horario Secundaria Manana", nivelSecundaria, turnoManana,
                        LocalTime.of(6, 45), LocalTime.of(7, 15), 10, LocalTime.of(7, 45), LocalTime.of(13, 45), LocalTime.of(14, 15)));
            }

            // 9. Estudiantes, Apoderados y Matriculas
            log.info("Asegurando datos de prueba para padres, estudiantes e inasistencias...");

            // Apoderado Principal: Juan Carlos Quispe Flores (DNI: 10234567 / clave: padre123)
            String dniPadre = "10234567";
            Usuario usuarioPadre = usuarioRepository.findByUsername(dniPadre).orElseGet(() -> {
                Usuario u = new Usuario(dniPadre, passwordEncoder.encode("padre123"), "Juan Quispe Flores (Padre)", "murgaalexander2@gmail.com");
                u.setTelefono("+51 974 555 666");
                if (rolPadre != null) u.agregarRol(rolPadre);
                return usuarioRepository.save(u);
            });
            // Si ya existía de ejecuciones previas, asegurar que su correo sea el especificado para pruebas
            if (usuarioPadre != null && !"murgaalexander2@gmail.com".equalsIgnoreCase(usuarioPadre.getEmail())) {
                usuarioPadre.setEmail("murgaalexander2@gmail.com");
                usuarioRepository.save(usuarioPadre);
            }

            Apoderado padreQuispe = apoderadoRepository.findByDni(dniPadre).orElseGet(() -> {
                Apoderado a = new Apoderado(dniPadre, "Juan Carlos", "Quispe Flores", "PADRE", "+51 974 555 666");
                a.setDireccion("Calle San Martin 145, Tuman");
                a.setEmail("murgaalexander2@gmail.com");
                a.setUsuario(usuarioPadre);
                return apoderadoRepository.save(a);
            });
            if (padreQuispe != null && !"murgaalexander2@gmail.com".equalsIgnoreCase(padreQuispe.getEmail())) {
                padreQuispe.setEmail("murgaalexander2@gmail.com");
                apoderadoRepository.save(padreQuispe);
            }

            // Hijos para Juan Quispe (Lucas, Mateo, Thiago, Camila, Joaquin, Rodrigo)
            Estudiante lucas = asegurarEstudiante(estudianteRepository, "71234567", "Lucas", "Quispe Mendoza", "M", "Calle San Martin 145, Tuman");
            Estudiante mateo = asegurarEstudiante(estudianteRepository, "71234568", "Mateo", "Quispe Mendoza", "M", "Calle San Martin 145, Tuman");
            Estudiante thiago = asegurarEstudiante(estudianteRepository, "71234571", "Thiago", "Quispe Mendoza", "M", "Calle San Martin 145, Tuman");
            Estudiante camila = asegurarEstudiante(estudianteRepository, "71234572", "Camila", "Quispe Mendoza", "F", "Calle San Martin 145, Tuman");
            Estudiante joaquin = asegurarEstudiante(estudianteRepository, "71234573", "Joaquin", "Quispe Mendoza", "M", "Calle San Martin 145, Tuman");
            Estudiante rodrigo = asegurarEstudiante(estudianteRepository, "71234574", "Rodrigo", "Quispe Mendoza", "M", "Calle San Martin 145, Tuman");

            padreQuispe.agregarEstudiante(lucas);
            padreQuispe.agregarEstudiante(mateo);
            padreQuispe.agregarEstudiante(thiago);
            padreQuispe.agregarEstudiante(camila);
            padreQuispe.agregarEstudiante(joaquin);
            padreQuispe.agregarEstudiante(rodrigo);
            padreQuispe = apoderadoRepository.save(padreQuispe);

            // Obtener Grados y Secciones de Primaria
            Grado g1Primaria = gradoRepository.findByNombreAndNivelCodigo("1o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);
            Grado g2Primaria = gradoRepository.findByNombreAndNivelCodigo("2o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);
            Grado g3Primaria = gradoRepository.findByNombreAndNivelCodigo("3o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);
            Grado g4Primaria = gradoRepository.findByNombreAndNivelCodigo("4o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);
            Grado g5Primaria = gradoRepository.findByNombreAndNivelCodigo("5o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);
            Grado g6Primaria = gradoRepository.findByNombreAndNivelCodigo("6o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);

            Seccion sec1A = g1Primaria != null ? seccionRepository.findByGradoId(g1Primaria.getId()).stream().findFirst().orElse(null) : null;
            Seccion sec2A = g2Primaria != null ? seccionRepository.findByGradoId(g2Primaria.getId()).stream().findFirst().orElse(null) : null;
            Seccion sec3A = g3Primaria != null ? seccionRepository.findByGradoId(g3Primaria.getId()).stream().findFirst().orElse(null) : null;
            Seccion sec4A = g4Primaria != null ? seccionRepository.findByGradoId(g4Primaria.getId()).stream().findFirst().orElse(null) : null;
            Seccion sec5A = g5Primaria != null ? seccionRepository.findByGradoId(g5Primaria.getId()).stream().findFirst().orElse(null) : null;
            Seccion sec6A = g6Primaria != null ? seccionRepository.findByGradoId(g6Primaria.getId()).stream().findFirst().orElse(null) : null;

            Matricula matLucas = asegurarMatricula(matriculaRepository, "MAT-2026-001", lucas, anio2026, sec1A, turnoManana);
            Matricula matMateo = asegurarMatricula(matriculaRepository, "MAT-2026-002", mateo, anio2026, sec3A, turnoManana);
            Matricula matThiago = asegurarMatricula(matriculaRepository, "MAT-2026-005", thiago, anio2026, sec5A, turnoManana);
            Matricula matCamila = asegurarMatricula(matriculaRepository, "MAT-2026-006", camila, anio2026, sec2A, turnoManana);
            Matricula matJoaquin = asegurarMatricula(matriculaRepository, "MAT-2026-007", joaquin, anio2026, sec4A, turnoManana);
            Matricula matRodrigo = asegurarMatricula(matriculaRepository, "MAT-2026-008", rodrigo, anio2026, sec6A, turnoManana);

            // Apoderada 2: Maria Elena Silva Castro (DNI: 10234568 / padre123) -> Madre de Valeria Torres
            Estudiante valeria = asegurarEstudiante(estudianteRepository, "71234569", "Valeria", "Torres Silva", "F", "Av. Ramon Castilla 320, Tuman");
            Grado g1Secundaria = gradoRepository.findByNombreAndNivelCodigo("1o Ano de Secundaria", EnumNivel.SECUNDARIA).orElse(null);
            Seccion secSec1A = g1Secundaria != null ? seccionRepository.findByGradoId(g1Secundaria.getId()).stream().findFirst().orElse(null) : null;
            Matricula matValeria = asegurarMatricula(matriculaRepository, "MAT-2026-003", valeria, anio2026, secSec1A, turnoManana);

            String dniMadre = "10234568";
            Usuario userMadre = usuarioRepository.findByUsername(dniMadre).orElseGet(() -> {
                Usuario u = new Usuario(dniMadre, passwordEncoder.encode("padre123"), "Maria Elena Silva Castro (Madre)", "maria.silva@gmail.com");
                u.setTelefono("+51 974 888 999");
                if (rolPadre != null) u.agregarRol(rolPadre);
                return usuarioRepository.save(u);
            });
            Apoderado madreSilva = apoderadoRepository.findByDni(dniMadre).orElseGet(() -> {
                Apoderado m = new Apoderado(dniMadre, "Maria Elena", "Silva Castro", "MADRE", "+51 974 888 999");
                m.setDireccion("Av. Ramon Castilla 320, Tuman");
                m.setUsuario(userMadre);
                return apoderadoRepository.save(m);
            });
            madreSilva.agregarEstudiante(valeria);
            apoderadoRepository.save(madreSilva);

            // Apoderado 3: Roberto Carlos Paredes Ruiz (DNI: 10234569 / padre123) -> Padre de Sofia Paredes
            Estudiante sofia = asegurarEstudiante(estudianteRepository, "71234570", "Sofia Nicole", "Paredes Diaz", "F", "Jr. Comercio 512, Tuman");
            Grado gInicial5 = gradoRepository.findByNombreAndNivelCodigo("Inicial 5 Anos", EnumNivel.INICIAL).orElse(null);
            Seccion secIni5A = gInicial5 != null ? seccionRepository.findByGradoId(gInicial5.getId()).stream().findFirst().orElse(null) : null;
            Matricula matSofia = asegurarMatricula(matriculaRepository, "MAT-2026-004", sofia, anio2026, secIni5A, turnoManana);

            String dniParedes = "10234569";
            Usuario userParedes = usuarioRepository.findByUsername(dniParedes).orElseGet(() -> {
                Usuario u = new Usuario(dniParedes, passwordEncoder.encode("padre123"), "Roberto Carlos Paredes Ruiz (Padre)", "roberto.paredes@gmail.com");
                u.setTelefono("+51 974 777 888");
                if (rolPadre != null) u.agregarRol(rolPadre);
                return usuarioRepository.save(u);
            });
            Apoderado padreParedes = apoderadoRepository.findByDni(dniParedes).orElseGet(() -> {
                Apoderado p = new Apoderado(dniParedes, "Roberto Carlos", "Paredes Ruiz", "PADRE", "+51 974 777 888");
                p.setDireccion("Jr. Comercio 512, Tuman");
                p.setUsuario(userParedes);
                return apoderadoRepository.save(p);
            });
            padreParedes.agregarEstudiante(sofia);
            apoderadoRepository.save(padreParedes);

            // 10. Registros de Asistencia: HOY (Estadisticas de Dashboard)
            LocalDate hoy = FechaUtil.hoyPeru();
            // Lucas Quispe se mantiene disponible y sin registro de hoy para permitir pruebas inmediatas en el escáner
            if (sofia != null && matSofia != null && asistenciaRepository.findByEstudianteIdAndFecha(sofia.getId(), hoy).isEmpty()) {
                asistenciaRepository.save(new RegistroAsistencia(sofia, anio2026, matSofia, hoy, LocalTime.of(7, 20), EstadoPuntualidad.PUNTUAL, auxiliarUser));
            }
            if (valeria != null && matValeria != null && asistenciaRepository.findByEstudianteIdAndFecha(valeria.getId(), hoy).isEmpty()) {
                RegistroAsistencia regValeria = new RegistroAsistencia(valeria, anio2026, matValeria, hoy, LocalTime.of(7, 35), EstadoPuntualidad.TARDANZA, auxiliarUser);
                regValeria.setMinutosTardanza(20);
                asistenciaRepository.save(regValeria);
            }

            // 11. Registros de Inasistencia dentro del plazo legal de 48h (Ayer)
            LocalDate ayer = hoy.minusDays(1);

            // Mateo: Inasistencia Ayer CON justificacion enviada (Ejemplo de solicitud en revision)
            RegistroAsistencia inasistenciaMateo = asegurarInasistencia(asistenciaRepository, mateo, anio2026, matMateo, ayer, auxiliarUser);
            if (inasistenciaMateo != null && justificacionRepository.findByRegistroAsistenciaId(inasistenciaMateo.getId()).isEmpty()) {
                JustificacionInasistencia justificacion = new JustificacionInasistencia(
                        inasistenciaMateo,
                        padreQuispe,
                        "Cita medica odontologica programada en el Centro de Salud de Tuman",
                        "constancia_medica_mateo.pdf"
                );
                justificacionRepository.save(justificacion);
            }

            // Alumnos de Juan Quispe con INASISTENCIA SIN JUSTIFICAR (Dentro de 48h)
            RegistroAsistencia asisLucas = asegurarInasistencia(asistenciaRepository, lucas, anio2026, matLucas, ayer, auxiliarUser);
            limpiarJustificacion(justificacionRepository, asisLucas);

            RegistroAsistencia asisThiago = asegurarInasistencia(asistenciaRepository, thiago, anio2026, matThiago, ayer, auxiliarUser);
            limpiarJustificacion(justificacionRepository, asisThiago);

            RegistroAsistencia asisCamila = asegurarInasistencia(asistenciaRepository, camila, anio2026, matCamila, ayer, auxiliarUser);
            limpiarJustificacion(justificacionRepository, asisCamila);

            RegistroAsistencia asisJoaquin = asegurarInasistencia(asistenciaRepository, joaquin, anio2026, matJoaquin, ayer, auxiliarUser);
            limpiarJustificacion(justificacionRepository, asisJoaquin);

            RegistroAsistencia asisRodrigo = asegurarInasistencia(asistenciaRepository, rodrigo, anio2026, matRodrigo, ayer, auxiliarUser);
            limpiarJustificacion(justificacionRepository, asisRodrigo);

            // Inasistencias adicionales para otros apoderados
            RegistroAsistencia asisValeriaAyer = asegurarInasistencia(asistenciaRepository, valeria, anio2026, matValeria, ayer, auxiliarUser);
            limpiarJustificacion(justificacionRepository, asisValeriaAyer);

            RegistroAsistencia asisSofiaAyer = asegurarInasistencia(asistenciaRepository, sofia, anio2026, matSofia, ayer, auxiliarUser);
            limpiarJustificacion(justificacionRepository, asisSofiaAyer);

            // 12. Tareas Escolares de Ejemplo (Módulo de Profesores)
            if (tareaRepository.count() == 0 && g1Primaria != null && sec1A != null && profesorUser != null) {
                TareaEscolar tareaMat = new TareaEscolar(
                        "Ficha N° 4: Operaciones de Suma y Resta con Llevada",
                        "Matemáticas",
                        "Desarrollar los problemas 1 al 10 de la ficha de trabajo entregada en clase. Graficar las operaciones en el cuaderno y presentar con pulcritud.",
                        g1Primaria,
                        sec1A,
                        profesorUser,
                        hoy.plusDays(2)
                );
                tareaRepository.save(tareaMat);

                TareaEscolar tareaCom = new TareaEscolar(
                        "Comprensión Lectora: 'El Árbol Generoso'",
                        "Comunicación Integral",
                        "Leer el cuento de la página 32 y responder las 4 preguntas de inferencia en el cuaderno de trabajo.",
                        g1Primaria,
                        sec1A,
                        profesorUser,
                        hoy.plusDays(3)
                );
                tareaRepository.save(tareaCom);
            }

            // 13. Comunicados Oficiales de Aula (Módulo de Tutores)
            if (comunicadoRepository.count() == 0 && g1Primaria != null && sec1A != null && tutorUser != null) {
                ComunicadoAula comUrgente = new ComunicadoAula(
                        "Reunión Extraordinaria de Entrega de Informes y Boletas",
                        "Estimados padres de familia de 1° de Primaria: Se les convoca con carácter de URGENCIA a la asamblea este viernes a las 4:30 PM en el aula para la entrega de boletas de notas y coordinación pedagógica.",
                        "URGENTE",
                        g1Primaria,
                        sec1A,
                        tutorUser
                );
                comunicadoRepository.save(comUrgente);

                ComunicadoAula comCampana = new ComunicadoAula(
                        "Campaña Preventiva de Salud Bucal y Fluorización",
                        "Se informa que el martes próximo se realizará la jornada gratuita de despistaje y fluorización con el personal del Centro de Salud. Por favor enviar el cepillo dental del estudiante.",
                        "NORMAL",
                        g1Primaria,
                        sec1A,
                        tutorUser
                );
                comunicadoRepository.save(comCampana);
            }

            // 14. Reportes de Conducta Escolar (Módulo de Tutores)
            if (reporteConductaRepository.count() == 0 && lucas != null && tutorUser != null) {
                ReporteConducta rep = new ReporteConducta(
                        lucas,
                        tutorUser,
                        "LLAMADA_ATENCION",
                        "Distracción en horario de clase y olvido de materiales",
                        "El estudiante estuvo distraído durante la clase de matemáticas conversando y no trajo su cartuchera de útiles por segunda vez consecutiva."
                );
                rep.setMedidasTomadas("Se dialogó con el estudiante en privado, comprometiéndose a prestar atención y revisar su mochila antes de salir de casa.");
                rep.setRequiereCitacion(false);
                reporteConductaRepository.save(rep);
            }

            log.info("Datos de inicialización completados exitosamente (Usuarios, Tareas, Comunicados, Conducta y Roles).");
        };
    }

    private Estudiante asegurarEstudiante(EstudianteRepository repo, String dni, String nombres, String apellidos, String genero, String direccion) {
        return repo.findByDni(dni).orElseGet(() -> {
            Estudiante e = new Estudiante(dni, nombres, apellidos, genero);
            e.setDireccion(direccion);
            return repo.save(e);
        });
    }

    private Matricula asegurarMatricula(MatriculaRepository matRepo, String codigo, Estudiante est, AnioEscolar anio, Seccion seccion, Turno turno) {
        if (est == null) return null;
        return matRepo.findByEstudianteDniAndAnioEscolarActivoTrue(est.getDni()).orElseGet(() -> {
            if (seccion != null && anio != null && turno != null) {
                return matRepo.save(new Matricula(codigo, est, anio, seccion, turno));
            }
            return null;
        });
    }

    private RegistroAsistencia asegurarInasistencia(
            RegistroAsistenciaRepository asisRepo,
            Estudiante est,
            AnioEscolar anio,
            Matricula mat,
            LocalDate fecha,
            Usuario auxiliarUser) {
        if (est == null || mat == null) return null;
        RegistroAsistencia asis = asisRepo.findByEstudianteIdAndFecha(est.getId(), fecha).orElse(null);
        if (asis == null) {
            asis = new RegistroAsistencia(est, anio, mat, fecha, null, EstadoPuntualidad.INASISTENCIA, auxiliarUser);
            asis.setObservaciones("Inasistencia registrada en control de puerta (sin marcar)");
            asis = asisRepo.save(asis);
        } else {
            asis.setEstado(EstadoPuntualidad.INASISTENCIA);
            asis.setHoraEntrada(null);
            asis.setObservaciones("Inasistencia habilitada para prueba de justificacion (48h)");
            asis = asisRepo.save(asis);
        }
        return asis;
    }

    private void limpiarJustificacion(JustificacionInasistenciaRepository justRepo, RegistroAsistencia asis) {
        if (asis != null && asis.getId() != null) {
            justRepo.findByRegistroAsistenciaId(asis.getId()).ifPresent(justRepo::delete);
        }
    }

    private void crearGradoYSeccion(GradoRepository gradoRepo, SeccionRepository seccionRepo,
                                   String nombreGrado, Integer numero, NivelEducativo nivel, String nombreSeccion) {
        Grado grado = gradoRepo.save(new Grado(nombreGrado, numero, nivel));
        seccionRepo.save(new Seccion(nombreSeccion, "Aula " + nombreGrado, grado));
    }
}
