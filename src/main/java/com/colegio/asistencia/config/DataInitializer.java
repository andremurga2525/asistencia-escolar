package com.colegio.asistencia.config;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
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
            JustificacionInasistenciaRepository justificacionRepository) {
        return args -> {
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
            }

            // 2. Roles del Sistema Escolar
            for (EnumRol enumRol : EnumRol.values()) {
                if (rolRepository.findByNombre(enumRol).isEmpty()) {
                    Rol rol = new Rol(enumRol, enumRol.getDescripcion());
                    rolRepository.save(rol);
                }
            }

            // 3. Usuarios Principales (Admin y Auxiliar)
            Rol rolAdmin = rolRepository.findByNombre(EnumRol.ROLE_ADMIN).orElse(null);
            Rol rolAuxiliar = rolRepository.findByNombre(EnumRol.ROLE_AUXILIAR).orElse(null);
            Rol rolPadre = rolRepository.findByNombre(EnumRol.ROLE_PADRE).orElse(null);

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

            if (gradoRepository.count() == 0 && nivelPrimaria != null && nivelSecundaria != null) {
                NivelEducativo nivelInicial = nivelRepository.findByCodigo(EnumNivel.INICIAL).orElse(null);
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
                NivelEducativo nivelInicial = nivelRepository.findByCodigo(EnumNivel.INICIAL).orElse(null);
                if (nivelInicial != null) {
                    horarioRepository.save(new HorarioEscolar("Horario Inicial Manana", nivelInicial, turnoManana,
                            LocalTime.of(7, 30), LocalTime.of(8, 0), 10, LocalTime.of(8, 30), LocalTime.of(12, 30), LocalTime.of(13, 0)));
                }
                horarioRepository.save(new HorarioEscolar("Horario Primaria Manana", nivelPrimaria, turnoManana,
                        LocalTime.of(7, 0), LocalTime.of(7, 30), 10, LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(13, 30)));
                horarioRepository.save(new HorarioEscolar("Horario Secundaria Manana", nivelSecundaria, turnoManana,
                        LocalTime.of(6, 45), LocalTime.of(7, 15), 10, LocalTime.of(7, 45), LocalTime.of(13, 45), LocalTime.of(14, 15)));
            }

            // 9. Estudiantes, Apoderados y Matriculas Iniciales
            if (estudianteRepository.count() == 0 && nivelPrimaria != null && turnoManana != null) {
                log.info("Creando estudiantes, apoderados y matriculas 2026...");

                // Estudiante 1 (Lucas)
                Estudiante lucas = new Estudiante("71234567", "Lucas", "Quispe Mendoza", "M");
                lucas.setDireccion("Calle San Martin 145, Tuman");
                lucas = estudianteRepository.save(lucas);

                // Estudiante 2 (Mateo)
                Estudiante mateo = new Estudiante("71234568", "Mateo", "Quispe Mendoza", "M");
                mateo.setDireccion("Calle San Martin 145, Tuman");
                mateo = estudianteRepository.save(mateo);

                // Estudiante 3 (Valeria)
                Estudiante valeria = new Estudiante("71234569", "Valeria", "Torres Silva", "F");
                valeria.setDireccion("Av. Ramon Castilla 320, Tuman");
                valeria = estudianteRepository.save(valeria);

                // Apoderado 1: Juan Quispe Flores (DNI: 10234567 / padre123)
                String dniPadre = "10234567";
                Usuario usuarioPadre = null;
                if (!usuarioRepository.existsByUsername(dniPadre) && rolPadre != null) {
                    usuarioPadre = new Usuario(dniPadre, passwordEncoder.encode("padre123"), "Juan Quispe Flores (Padre)", "padre.quispe@gmail.com");
                    usuarioPadre.setTelefono("+51 974 555 666");
                    usuarioPadre.agregarRol(rolPadre);
                    usuarioPadre = usuarioRepository.save(usuarioPadre);
                } else {
                    usuarioPadre = usuarioRepository.findByUsername(dniPadre).orElse(null);
                }

                Apoderado padreQuispe = new Apoderado(dniPadre, "Juan Carlos", "Quispe Flores", "PADRE", "+51 974 555 666");
                padreQuispe.setDireccion("Calle San Martin 145, Tuman");
                padreQuispe.setUsuario(usuarioPadre);
                padreQuispe.agregarEstudiante(lucas);
                padreQuispe.agregarEstudiante(mateo);
                apoderadoRepository.save(padreQuispe);

                // Matriculas en Grados y Secciones
                Grado grado1Primaria = gradoRepository.findByNombreAndNivelCodigo("1o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);
                Grado grado3Primaria = gradoRepository.findByNombreAndNivelCodigo("3o Grado de Primaria", EnumNivel.PRIMARIA).orElse(null);
                Grado grado1Secundaria = gradoRepository.findByNombreAndNivelCodigo("1o Ano de Secundaria", EnumNivel.SECUNDARIA).orElse(null);

                Seccion seccion1A = grado1Primaria != null ? seccionRepository.findByGradoId(grado1Primaria.getId()).stream().findFirst().orElse(null) : null;
                Seccion seccion3A = grado3Primaria != null ? seccionRepository.findByGradoId(grado3Primaria.getId()).stream().findFirst().orElse(null) : null;
                Seccion seccionSec1A = grado1Secundaria != null ? seccionRepository.findByGradoId(grado1Secundaria.getId()).stream().findFirst().orElse(null) : null;

                if (seccion1A != null) {
                    matriculaRepository.save(new Matricula("MAT-2026-001", lucas, anio2026, seccion1A, turnoManana));
                }
                if (seccion3A != null) {
                    matriculaRepository.save(new Matricula("MAT-2026-002", mateo, anio2026, seccion3A, turnoManana));
                }
                if (seccionSec1A != null) {
                    matriculaRepository.save(new Matricula("MAT-2026-003", valeria, anio2026, seccionSec1A, turnoManana));
                }
            }

            // 9.1 Asegurar que existan cuentas de acceso para múltiples padres de prueba:
            // Apoderada 2: María Elena Silva Castro (DNI: 10234568 / padre123) -> Madre de Valeria Torres
            if (apoderadoRepository.findByDni("10234568").isEmpty()) {
                String dniMadre = "10234568";
                Usuario userMadre = usuarioRepository.findByUsername(dniMadre).orElseGet(() -> {
                    Usuario u = new Usuario(dniMadre, passwordEncoder.encode("padre123"), "Maria Elena Silva Castro (Madre)", "maria.silva@gmail.com");
                    u.setTelefono("+51 974 888 999");
                    u.agregarRol(rolPadre);
                    return usuarioRepository.save(u);
                });
                Apoderado madreSilva = new Apoderado(dniMadre, "Maria Elena", "Silva Castro", "MADRE", "+51 974 888 999");
                madreSilva.setDireccion("Av. Ramon Castilla 320, Tuman");
                madreSilva.setUsuario(userMadre);
                estudianteRepository.findByDni("71234569").ifPresent(madreSilva::agregarEstudiante);
                apoderadoRepository.save(madreSilva);
                log.info("Apoderada Maria Elena Silva creada (DNI: 10234568 / padre123) vinculada a Valeria Torres.");
            }

            // Apoderado 3: Roberto Carlos Paredes Ruiz (DNI: 10234569 / padre123) -> Padre de Sofia Paredes (Inicial 5 Anos)
            if (estudianteRepository.findByDni("71234570").isEmpty()) {
                Estudiante sofia = new Estudiante("71234570", "Sofia Nicole", "Paredes Diaz", "F");
                sofia.setDireccion("Jr. Comercio 512, Tuman");
                sofia = estudianteRepository.save(sofia);

                Grado gradoInicial5 = gradoRepository.findByNombreAndNivelCodigo("Inicial 5 Anos", EnumNivel.INICIAL).orElse(null);
                Seccion seccionInicial5A = gradoInicial5 != null ? seccionRepository.findByGradoId(gradoInicial5.getId()).stream().findFirst().orElse(null) : null;
                if (seccionInicial5A != null && anio2026 != null && turnoManana != null) {
                    matriculaRepository.save(new Matricula("MAT-2026-004", sofia, anio2026, seccionInicial5A, turnoManana));
                }

                String dniParedes = "10234569";
                Usuario userParedes = usuarioRepository.findByUsername(dniParedes).orElseGet(() -> {
                    Usuario u = new Usuario(dniParedes, passwordEncoder.encode("padre123"), "Roberto Carlos Paredes Ruiz (Padre)", "roberto.paredes@gmail.com");
                    u.setTelefono("+51 974 777 888");
                    u.agregarRol(rolPadre);
                    return usuarioRepository.save(u);
                });
                Apoderado padreParedes = new Apoderado(dniParedes, "Roberto Carlos", "Paredes Ruiz", "PADRE", "+51 974 777 888");
                padreParedes.setDireccion("Jr. Comercio 512, Tuman");
                padreParedes.setUsuario(userParedes);
                padreParedes.agregarEstudiante(sofia);
                apoderadoRepository.save(padreParedes);
                log.info("Apoderado Roberto Paredes creado (DNI: 10234569 / padre123) vinculado a Sofia Paredes.");
            }

            // 10. Registros de Asistencia y Justificaciones para HOY y AYER
            LocalDate hoy = LocalDate.now();
            LocalDate ayer = hoy.minusDays(1);

            Estudiante lucas = estudianteRepository.findByDni("71234567").orElse(null);
            Estudiante mateo = estudianteRepository.findByDni("71234568").orElse(null);
            Estudiante valeria = estudianteRepository.findByDni("71234569").orElse(null);
            Apoderado padreQuispe = apoderadoRepository.findByDni("10234567").orElse(null);

            Matricula matLucas = lucas != null ? matriculaRepository.findByEstudianteDniAndAnioEscolarActivoTrue("71234567").orElse(null) : null;
            Matricula matMateo = mateo != null ? matriculaRepository.findByEstudianteDniAndAnioEscolarActivoTrue("71234568").orElse(null) : null;
            Matricula matValeria = valeria != null ? matriculaRepository.findByEstudianteDniAndAnioEscolarActivoTrue("71234569").orElse(null) : null;

            if (lucas != null && matLucas != null && asistenciaRepository.findByEstudianteIdAndFecha(lucas.getId(), hoy).isEmpty()) {
                // Lucas: Hoy PUNTUAL (07:25)
                asistenciaRepository.save(new RegistroAsistencia(lucas, anio2026, matLucas, hoy, LocalTime.of(7, 25), EstadoPuntualidad.PUNTUAL, auxiliarUser));
            }

            if (valeria != null && matValeria != null && asistenciaRepository.findByEstudianteIdAndFecha(valeria.getId(), hoy).isEmpty()) {
                // Valeria: Hoy TARDANZA (07:35)
                RegistroAsistencia regValeria = new RegistroAsistencia(valeria, anio2026, matValeria, hoy, LocalTime.of(7, 35), EstadoPuntualidad.TARDANZA, auxiliarUser);
                regValeria.setMinutosTardanza(20);
                asistenciaRepository.save(regValeria);
            }

            if (mateo != null && matMateo != null && padreQuispe != null && asistenciaRepository.findByEstudianteIdAndFecha(mateo.getId(), ayer).isEmpty()) {
                // Mateo: Ayer INASISTENCIA (Dentro del plazo de 48 horas)
                RegistroAsistencia inasistenciaMateo = new RegistroAsistencia(mateo, anio2026, matMateo, ayer, null, EstadoPuntualidad.INASISTENCIA, auxiliarUser);
                inasistenciaMateo.setObservaciones("No se registro entrada en la puerta");
                inasistenciaMateo = asistenciaRepository.save(inasistenciaMateo);

                // Justificacion enviada por el padre dentro del plazo de 48 horas
                JustificacionInasistencia justificacion = new JustificacionInasistencia(
                        inasistenciaMateo,
                        padreQuispe,
                        "Cita medica odontologica programada en el Centro de Salud de Tuman",
                        "constancia_medica_mateo.pdf"
                );
                justificacionRepository.save(justificacion);
                log.info("Justificacion de inasistencia para Mateo registrada dentro del plazo de 48h.");
            }
        };
    }

    private void crearGradoYSeccion(GradoRepository gradoRepo, SeccionRepository seccionRepo,
                                   String nombreGrado, Integer numero, NivelEducativo nivel, String nombreSeccion) {
        Grado grado = gradoRepo.save(new Grado(nombreGrado, numero, nivel));
        seccionRepo.save(new Seccion(nombreSeccion, "Aula " + nombreGrado, grado));
    }
}
