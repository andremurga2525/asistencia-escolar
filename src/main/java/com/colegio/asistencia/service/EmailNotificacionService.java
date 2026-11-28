package com.colegio.asistencia.service;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.ApoderadoRepository;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import com.colegio.asistencia.repository.MatriculaRepository;
import com.colegio.asistencia.util.FechaUtil;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

@Service
public class EmailNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificacionService.class);

    private final JavaMailSender defaultMailSender;
    private final ApoderadoRepository apoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final ConfiguracionColegioRepository configRepository;

    @Value("${spring.mail.username:notificaciones.jeshua@gmail.com}")
    private String defaultRemitente;

    @Value("${spring.mail.password:}")
    private String defaultPassword;

    public EmailNotificacionService(
            @Autowired(required = false) JavaMailSender defaultMailSender,
            ApoderadoRepository apoderadoRepository,
            MatriculaRepository matriculaRepository,
            ConfiguracionColegioRepository configRepository) {
        this.defaultMailSender = defaultMailSender;
        this.apoderadoRepository = apoderadoRepository;
        this.matriculaRepository = matriculaRepository;
        this.configRepository = configRepository;
    }

    /**
     * Resuelve el sender SMTP adecuado: si el Administrador configuró su propio Gmail
     * en el panel institucional (base de datos), se construye un JavaMailSender dinámico.
     * De lo contrario, se usa la configuración por defecto de Spring Boot.
     */
    private JavaMailSender resolverMailSender(ConfiguracionColegio config) {
        if (config != null && config.getEmailNotificaciones() != null && !config.getEmailNotificaciones().trim().isEmpty()
                && config.getEmailPassword() != null && !config.getEmailPassword().trim().isEmpty()) {
            JavaMailSenderImpl impl = new JavaMailSenderImpl();
            impl.setHost("smtp.gmail.com");
            impl.setPort(587);
            impl.setUsername(config.getEmailNotificaciones().trim());
            impl.setPassword(config.getEmailPassword().trim().replace(" ", ""));

            Properties props = impl.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "8000");
            props.put("mail.smtp.timeout", "8000");
            props.put("mail.smtp.writetimeout", "8000");
            return impl;
        }
        return this.defaultMailSender;
    }

    private String resolverRemitente(ConfiguracionColegio config) {
        if (config != null && config.getEmailNotificaciones() != null && !config.getEmailNotificaciones().trim().isEmpty()) {
            return config.getEmailNotificaciones().trim();
        }
        return this.defaultRemitente;
    }

    /**
     * Notificación asíncrona enviada a los apoderados al escanear la ENTRADA del alumno en puerta.
     */
    @Async
    public void notificarEntrada(RegistroAsistencia registro) {
        if (registro == null || registro.getEstudiante() == null) return;

        List<Apoderado> apoderados = apoderadoRepository.findByEstudiantesId(registro.getEstudiante().getId());
        String nombreAlumno = registro.getEstudiante().getNombreCompleto();
        String hora = registro.getHoraEntrada() != null 
                ? registro.getHoraEntrada().format(DateTimeFormatter.ofPattern("HH:mm:ss")) 
                : "No registrada";
        String fecha = registro.getFecha() != null 
                ? registro.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                : "";

        boolean esTardanza = registro.getEstado() == EstadoPuntualidad.TARDANZA;
        String estadoTexto = esTardanza 
                ? "TARDANZA (" + registro.getMinutosTardanza() + " min tarde)" 
                : "PUNTUAL";
        String colorEstado = esTardanza ? "#ca8a04" : "#16a34a";

        for (Apoderado apoderado : apoderados) {
            String destino = obtenerEmailValido(apoderado);
            if (destino == null) continue;

            String asunto = "🔔 Notificación de Ingreso: " + nombreAlumno + " - I.E.P. Internacional Jeshua";
            String cuerpoHtml = construirPlantillaHtml(
                    "Registro de Ingreso a Clases",
                    "Estimado(a) " + apoderado.getNombreCompleto() + ":",
                    "Le informamos que su hijo(a) ha registrado su <strong>INGRESO</strong> al plantel escolar.",
                    nombreAlumno,
                    fecha,
                    hora,
                    estadoTexto,
                    colorEstado,
                    "Registro realizado en la puerta principal mediante escaneo del fotocheck con código QR."
            );

            enviarCorreo(destino, asunto, cuerpoHtml);
        }
    }

    /**
     * Notificación asíncrona enviada a los apoderados al escanear la SALIDA del alumno en puerta.
     */
    @Async
    public void notificarSalida(RegistroAsistencia registro) {
        if (registro == null || registro.getEstudiante() == null) return;

        List<Apoderado> apoderados = apoderadoRepository.findByEstudiantesId(registro.getEstudiante().getId());
        String nombreAlumno = registro.getEstudiante().getNombreCompleto();
        String hora = registro.getHoraSalida() != null 
                ? registro.getHoraSalida().format(DateTimeFormatter.ofPattern("HH:mm:ss")) 
                : "No registrada";
        String fecha = registro.getFecha() != null 
                ? registro.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                : "";

        for (Apoderado apoderado : apoderados) {
            String destino = obtenerEmailValido(apoderado);
            if (destino == null) continue;

            String asunto = "🚪 Notificación de Salida: " + nombreAlumno + " - I.E.P. Internacional Jeshua";
            String cuerpoHtml = construirPlantillaHtml(
                    "Registro de Salida del Colegio",
                    "Estimado(a) " + apoderado.getNombreCompleto() + ":",
                    "Le informamos que su hijo(a) ha concluido su jornada y ha registrado su <strong>SALIDA</strong> del plantel escolar.",
                    nombreAlumno,
                    fecha,
                    hora,
                    "SALIDA REGISTRADA",
                    "#0284c7",
                    "Por favor tome las previsiones para la llegada segura del menor a su hogar."
            );

            enviarCorreo(destino, asunto, cuerpoHtml);
        }
    }

    /**
     * Notificación asíncrona cuando la Dirección aprueba o rechaza una justificación de inasistencia.
     */
    @Async
    public void notificarResolucionJustificacion(JustificacionInasistencia justificacion) {
        if (justificacion == null || justificacion.getApoderado() == null) return;

        Apoderado apoderado = justificacion.getApoderado();
        String destino = obtenerEmailValido(apoderado);
        if (destino == null) return;

        String nombreAlumno = justificacion.getRegistroAsistencia().getEstudiante().getNombreCompleto();
        String fechaFalta = justificacion.getRegistroAsistencia().getFecha() != null 
                ? justificacion.getRegistroAsistencia().getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                : "";

        boolean aprobada = justificacion.getEstado() == EnumEstadoJustificacion.APROBADA;
        String estadoTexto = aprobada ? "SOLICITUD APROBADA" : "SOLICITUD RECHAZADA";
        String colorEstado = aprobada ? "#16a34a" : "#dc2626";

        String asunto = "📋 Respuesta de Justificación: " + nombreAlumno + " - I.E.P. Internacional Jeshua";
        String mensaje = aprobada
                ? "La Dirección General ha revisado y <strong>APROBADO</strong> la justificación de inasistencia remitida para su hijo(a)."
                : "La Dirección General ha evaluado la justificación de inasistencia remitida para su hijo(a). Motivo de observación: " + 
                  (justificacion.getObservacionRevision() != null ? justificacion.getObservacionRevision() : "No adjuntó sustento médico válido.");

        String cuerpoHtml = construirPlantillaHtml(
                "Resolución de Justificación Escolar",
                "Estimado(a) " + apoderado.getNombreCompleto() + ":",
                mensaje,
                nombreAlumno,
                fechaFalta,
                "Trámite Resuelto",
                estadoTexto,
                colorEstado,
                "Observación de Dirección: " + (justificacion.getObservacionRevision() != null ? justificacion.getObservacionRevision() : "Conforme a reglamento interno.")
        );

        enviarCorreo(destino, asunto, cuerpoHtml);
    }

    /**
     * Notificación asíncrona cuando un profesor publica una tarea escolar.
     */
    @Async
    public void notificarTareaNueva(TareaEscolar tarea) {
        if (tarea == null || tarea.getGrado() == null || tarea.getSeccion() == null) return;

        List<Matricula> matriculas = matriculaRepository.findBySeccionIdAndAnioEscolarActivoTrue(tarea.getSeccion().getId());
        String fechaEntrega = tarea.getFechaEntrega() != null ? tarea.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
        String nombreDocente = tarea.getProfesor() != null ? tarea.getProfesor().getNombreCompleto() : "Docente";

        for (Matricula mat : matriculas) {
            if (mat.getEstudiante() == null) continue;
            List<Apoderado> apoderados = apoderadoRepository.findByEstudiantesId(mat.getEstudiante().getId());
            for (Apoderado ap : apoderados) {
                String destino = obtenerEmailValido(ap);
                if (destino == null) continue;

                String asunto = "📚 Nueva Tarea (" + tarea.getMateria() + "): " + mat.getEstudiante().getNombreCompleto();
                String cuerpo = construirPlantillaTareaHtml(
                        "Nueva Tarea Escolar Asignada",
                        "Estimado(a) " + ap.getNombreCompleto() + ":",
                        "Se ha registrado una nueva tarea escolar para su menor hijo(a).",
                        mat.getEstudiante().getNombreCompleto(),
                        tarea.getMateria(),
                        tarea.getTitulo(),
                        fechaEntrega,
                        tarea.getDescripcion(),
                        nombreDocente
                );
                enviarCorreo(destino, asunto, cuerpo);
            }
        }
    }

    /**
     * Notificación asíncrona cuando el tutor publica un comunicado de aula o institucional.
     */
    @Async
    public void notificarComunicado(ComunicadoAula comunicado) {
        if (comunicado == null) return;
        boolean esUrgente = comunicado.isUrgente();
        String prefijo = esUrgente ? "🚨 COMUNICADO URGENTE: " : "📢 Comunicado del Tutor: ";
        String asunto = prefijo + comunicado.getTitulo() + " - I.E.P. Internacional Jeshua";
        String nombreTutor = comunicado.getTutor() != null ? comunicado.getTutor().getNombreCompleto() : "Tutor de Aula";

        List<Apoderado> apoderados;
        if (comunicado.getSeccion() != null) {
            List<Matricula> matriculas = matriculaRepository.findBySeccionIdAndAnioEscolarActivoTrue(comunicado.getSeccion().getId());
            apoderados = matriculas.stream()
                    .filter(m -> m.getEstudiante() != null)
                    .flatMap(m -> apoderadoRepository.findByEstudiantesId(m.getEstudiante().getId()).stream())
                    .distinct()
                    .toList();
        } else {
            apoderados = apoderadoRepository.findAll();
        }

        for (Apoderado ap : apoderados) {
            String destino = obtenerEmailValido(ap);
            if (destino == null) continue;

            String cuerpo = construirPlantillaComunicadoHtml(
                    esUrgente ? "COMUNICADO URGENTE DEL TUTOR" : "Comunicado Oficial de Tutoría",
                    "Estimado(a) " + ap.getNombreCompleto() + ":",
                    comunicado.getTitulo(),
                    comunicado.getMensaje(),
                    comunicado.getPrioridad(),
                    nombreTutor
            );
            enviarCorreo(destino, asunto, cuerpo);
        }
    }

    /**
     * Notificación asíncrona cuando el tutor registra un reporte de conducta / falta disciplinaria para un alumno.
     */
    @Async
    public void notificarReporteConducta(ReporteConducta reporte) {
        if (reporte == null || reporte.getEstudiante() == null) return;
        List<Apoderado> apoderados = apoderadoRepository.findByEstudiantesId(reporte.getEstudiante().getId());
        String nombreAlumno = reporte.getEstudiante().getNombreCompleto();
        String fecha = reporte.getFechaIncidencia() != null ? reporte.getFechaIncidencia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
        String tutor = reporte.getTutor() != null ? reporte.getTutor().getNombreCompleto() : "Tutor";

        for (Apoderado ap : apoderados) {
            String destino = obtenerEmailValido(ap);
            if (destino == null) continue;

            String asunto = "⚠️ Reporte de Conducta e Incidencia: " + nombreAlumno + " - I.E.P. Internacional Jeshua";
            String cuerpo = construirPlantillaConductaHtml(
                    "Reporte de Conducta y Disciplina Escolar",
                    "Estimado(a) " + ap.getNombreCompleto() + ":",
                    "Se le informa formalmente sobre un reporte de conducta registrado para su menor hijo(a) en el plantel educativo.",
                    nombreAlumno,
                    reporte.getTipoFalta(),
                    reporte.getTitulo(),
                    fecha,
                    reporte.getDescripcion(),
                    reporte.getMedidasTomadas() != null ? reporte.getMedidasTomadas() : "Seguimiento en aula.",
                    Boolean.TRUE.equals(reporte.getRequiereCitacion()),
                    tutor
            );
            enviarCorreo(destino, asunto, cuerpo);
        }
    }

    /**
     * Permite probar el envío inmediato de correos hacia un correo específico (como murgaalexander2@gmail.com).
     */
    public void enviarCorreoPrueba(String destino) throws Exception {
        if (destino == null || !destino.contains("@")) {
            throw new IllegalArgumentException("Debe ingresar un correo electrónico válido para la prueba.");
        }

        ConfiguracionColegio config = configRepository.findFirstByActivoTrue().orElse(null);
        String fechaHora = FechaUtil.ahoraFechaHoraPeru().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String asunto = "🧪 Prueba de Notificación de Asistencia - I.E.P. Internacional Jeshua";
        String cuerpoHtml = construirPlantillaHtml(
                "Prueba del Sistema de Notificaciones por Correo",
                "Estimado(a) Apoderado / Administrador:",
                "¡Excelente! Este es un mensaje de prueba emitido satisfactoriamente desde la plataforma de control escolar. Indica que la conexión con el servidor de correo Gmail se encuentra 100% activa y configurada correctamente.",
                "Alumno de Prueba (Simulación)",
                FechaUtil.hoyPeru().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                FechaUtil.ahoraHoraPeru().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                "SISTEMA ACTIVO",
                "#16a34a",
                "Sello de tiempo oficial Perú: " + fechaHora + ". Las notificaciones automáticas de entrada, salida, tareas y comunicados llegarán a este correo."
        );

        JavaMailSender sender = resolverMailSender(config);
        String remitente = resolverRemitente(config);

        if (sender == null) {
            throw new IllegalStateException("No hay un servicio SMTP configurado. Por favor ingrese su Gmail y Contraseña de Aplicación en la sección de Correo.");
        }

        MimeMessage mensaje = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setFrom(remitente, "I.E.P. Internacional Jeshua");
        helper.setTo(destino.trim());
        helper.setSubject(asunto);
        helper.setText(cuerpoHtml, true);

        sender.send(mensaje);
        log.info("✅ Correo de prueba enviado satisfactoriamente a {}", destino);
    }

    private String obtenerEmailValido(Apoderado apoderado) {
        if (apoderado == null) return null;
        if (apoderado.getEmail() != null && !apoderado.getEmail().trim().isEmpty() && apoderado.getEmail().contains("@")) {
            return apoderado.getEmail().trim();
        }
        if (apoderado.getUsuario() != null && apoderado.getUsuario().getEmail() != null 
                && !apoderado.getUsuario().getEmail().trim().isEmpty() && apoderado.getUsuario().getEmail().contains("@")) {
            return apoderado.getUsuario().getEmail().trim();
        }
        return null;
    }

    private void enviarCorreo(String destinatario, String asunto, String cuerpoHtml) {
        log.info("📧 [NOTIFICACIÓN CORREO] Destinatario: {} | Asunto: {}", destinatario, asunto);

        try {
            ConfiguracionColegio config = configRepository.findFirstByActivoTrue().orElse(null);
            
            if (config != null && Boolean.FALSE.equals(config.getEmailActivo())) {
                log.info("ℹ️ Notificaciones por correo desactivadas en la configuración institucional.");
                return;
            }

            JavaMailSender sender = resolverMailSender(config);
            String remitente = resolverRemitente(config);

            boolean tieneClaveEnDb = config != null && config.getEmailPassword() != null && !config.getEmailPassword().trim().isEmpty();
            boolean tieneClaveEnProps = defaultPassword != null && !defaultPassword.trim().isEmpty();

            if (sender == null || (!tieneClaveEnDb && !tieneClaveEnProps)) {
                log.info("ℹ️ Servicio SMTP en modo Auditoría/Simulación (Configure Gmail y Contraseña de Aplicación en /admin/configuracion para envío real).");
                return;
            }

            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "I.E.P. Internacional Jeshua");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            sender.send(mensaje);
            log.info("✅ Correo enviado con éxito a {}", destinatario);
        } catch (Exception e) {
            log.error("⚠️ No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
        }
    }

    private String construirPlantillaHtml(
            String tituloPrincipal,
            String saludo,
            String mensajeDetalle,
            String nombreAlumno,
            String fecha,
            String hora,
            String estadoTexto,
            String colorEstado,
            String notaPie) {

        ConfiguracionColegio colegio = configRepository.findFirstByActivoTrue().orElse(null);
        String nombreCol = colegio != null ? colegio.getNombreColegio() : "I.E.P. Internacional Jeshua";
        String colorPrimario = colegio != null && colegio.getColorPrimario() != null ? colegio.getColorPrimario() : "#b91c1c";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333;'>" +
                "  <table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background-color: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.08);'>" +
                "    <tr>" +
                "      <td style='background-color: " + colorPrimario + "; padding: 25px 30px; text-align: center; color: #ffffff;'>" +
                "        <div style='margin-bottom: 10px;'>" +
                "          <img src='https://asistencia-escolar-production-afc5f81e.up.railway.app/logo-colegio' alt='Escudo del Colegio' style='max-height: 65px; max-width: 120px; object-fit: contain; background-color: #ffffff; padding: 4px; border-radius: 8px; box-shadow: 0 2px 6px rgba(0,0,0,0.2);' />" +
                "        </div>" +
                "        <h2 style='margin: 0; font-size: 21px; font-weight: bold; letter-spacing: 0.5px;'>" + nombreCol + "</h2>" +
                "        <p style='margin: 4px 0 0; font-size: 13px; color: #fecaca;'>Sistema Integrado de Asistencia y Notificaciones al Apoderado</p>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style='padding: 30px;'>" +
                "        <h3 style='margin: 0 0 15px; color: #111827; font-size: 18px;'>" + tituloPrincipal + "</h3>" +
                "        <p style='margin: 0 0 15px; font-size: 15px; line-height: 1.5;'>" + saludo + "</p>" +
                "        <p style='margin: 0 0 20px; font-size: 15px; line-height: 1.5;'>" + mensajeDetalle + "</p>" +
                "        <table width='100%' style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 15px; margin-bottom: 20px; font-size: 14px;'>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Estudiante:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + nombreAlumno + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Fecha:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + fecha + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Hora registrada:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + hora + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Estado:</td><td style='padding: 6px 0;'><span style='background-color: " + colorEstado + "; color: #ffffff; font-weight: bold; padding: 4px 12px; border-radius: 20px; font-size: 12px;'>" + estadoTexto + "</span></td></tr>" +
                "        </table>" +
                "        <p style='margin: 0 0 25px; font-size: 13px; color: #64748b; font-style: italic;'>" + notaPie + "</p>" +
                "        <div style='text-align: center; margin-bottom: 10px;'>" +
                "          <a href='https://asistencia-escolar-production-afc5f81e.up.railway.app/padre/hijos' style='background-color: " + colorPrimario + "; color: #ffffff; text-decoration: none; padding: 12px 25px; border-radius: 25px; font-weight: bold; font-size: 14px; display: inline-block;'>Ingresar al Portal de Padres</a>" +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style='background-color: #f1f5f9; padding: 15px 30px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;'>" +
                "        I.E.P. Internacional Jeshua • Tumán, Chiclayo, Lambayeque • Tel: +51 974 123 456" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    private String construirPlantillaTareaHtml(
            String tituloPrincipal,
            String saludo,
            String mensajeDetalle,
            String nombreAlumno,
            String materia,
            String tituloTarea,
            String fechaEntrega,
            String descripcion,
            String docente) {

        ConfiguracionColegio colegio = configRepository.findFirstByActivoTrue().orElse(null);
        String nombreCol = colegio != null ? colegio.getNombreColegio() : "I.E.P. Internacional Jeshua";
        String colorPrimario = colegio != null && colegio.getColorPrimario() != null ? colegio.getColorPrimario() : "#b91c1c";

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333;'>" +
                "  <table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background-color: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.08);'>" +
                "    <tr>" +
                "      <td style='background-color: " + colorPrimario + "; padding: 25px 30px; text-align: center; color: #ffffff;'>" +
                "        <img src='https://asistencia-escolar-production-afc5f81e.up.railway.app/logo-colegio' alt='Logo' style='max-height: 60px; max-width: 120px; object-fit: contain; background: #fff; padding: 4px; border-radius: 8px;' />" +
                "        <h2 style='margin: 8px 0 0; font-size: 20px; font-weight: bold;'>" + nombreCol + "</h2>" +
                "        <p style='margin: 4px 0 0; font-size: 13px; color: #fecaca;'>Módulo Académico de Tareas Escolares</p>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style='padding: 30px;'>" +
                "        <h3 style='margin: 0 0 15px; color: #1e293b; font-size: 18px;'>📚 " + tituloPrincipal + "</h3>" +
                "        <p style='margin: 0 0 15px; font-size: 15px; line-height: 1.5;'>" + saludo + "</p>" +
                "        <p style='margin: 0 0 20px; font-size: 15px; line-height: 1.5;'>" + mensajeDetalle + "</p>" +
                "        <table width='100%' style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 15px; margin-bottom: 20px; font-size: 14px;'>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Estudiante:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + nombreAlumno + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Curso / Materia:</td><td style='padding: 6px 0; font-weight: bold; color: #b91c1c;'>" + materia + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Actividad:</td><td style='padding: 6px 0; font-weight: bold;'>" + tituloTarea + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Fecha Límite:</td><td style='padding: 6px 0;'><span style='background-color: #fef08a; color: #854d0e; font-weight: bold; padding: 3px 10px; border-radius: 15px; font-size: 13px;'>📅 " + fechaEntrega + "</span></td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Docente:</td><td style='padding: 6px 0; color: #334155;'>" + docente + "</td></tr>" +
                "        </table>" +
                "        <div style='background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px;'>" +
                "          <strong style='color: #b45309; font-size: 13px;'>Instrucciones:</strong>" +
                "          <p style='margin: 5px 0 0; font-size: 14px; color: #451a03;'>" + (descripcion != null ? descripcion : "Ver detalles en la plataforma.") + "</p>" +
                "        </div>" +
                "        <div style='text-align: center; margin-top: 25px;'>" +
                "          <a href='https://asistencia-escolar-production-afc5f81e.up.railway.app/padre/hijos' style='background-color: #b91c1c; color: #ffffff; text-decoration: none; padding: 12px 25px; border-radius: 25px; font-weight: bold; font-size: 14px; display: inline-block;'>Ver Tarea y Descargar Materiales</a>" +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body></html>";
    }

    private String construirPlantillaComunicadoHtml(
            String tituloPrincipal,
            String saludo,
            String tituloComunicado,
            String mensaje,
            String prioridad,
            String tutor) {

        ConfiguracionColegio colegio = configRepository.findFirstByActivoTrue().orElse(null);
        String nombreCol = colegio != null ? colegio.getNombreColegio() : "I.E.P. Internacional Jeshua";
        boolean urgente = "URGENTE".equalsIgnoreCase(prioridad);
        String colorCabecera = urgente ? "#dc2626" : "#0284c7";

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333;'>" +
                "  <table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background-color: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.08);'>" +
                "    <tr>" +
                "      <td style='background-color: " + colorCabecera + "; padding: 25px 30px; text-align: center; color: #ffffff;'>" +
                "        <img src='https://asistencia-escolar-production-afc5f81e.up.railway.app/logo-colegio' alt='Logo' style='max-height: 60px; max-width: 120px; object-fit: contain; background: #fff; padding: 4px; border-radius: 8px;' />" +
                "        <h2 style='margin: 8px 0 0; font-size: 20px; font-weight: bold;'>" + nombreCol + "</h2>" +
                "        <p style='margin: 4px 0 0; font-size: 13px; color: #ffffff; opacity: 0.9;'>Comunicado Oficial de Tutoría y Convivencia</p>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style='padding: 30px;'>" +
                "        <div style='margin-bottom: 15px;'>" +
                "          <span style='background-color: " + (urgente ? "#fee2e2" : "#e0f2fe") + "; color: " + (urgente ? "#991b1b" : "#0369a1") + "; font-weight: bold; font-size: 12px; padding: 4px 12px; border-radius: 20px; text-transform: uppercase;'>" + (urgente ? "🚨 COMUNICADO URGENTE" : "📢 INFORMATIVO") + "</span>" +
                "        </div>" +
                "        <h3 style='margin: 0 0 15px; color: #111827; font-size: 19px;'>" + tituloComunicado + "</h3>" +
                "        <p style='margin: 0 0 15px; font-size: 15px; line-height: 1.5;'>" + saludo + "</p>" +
                "        <div style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 18px; margin-bottom: 20px; font-size: 15px; line-height: 1.6; color: #1e293b;'>" +
                mensaje.replace("\n", "<br>") +
                "        </div>" +
                "        <p style='font-size: 13px; color: #64748b; margin-bottom: 25px;'>Emitido por: <strong>" + tutor + "</strong> (Tutor de Aula)</p>" +
                "        <div style='text-align: center;'>" +
                "          <a href='https://asistencia-escolar-production-afc5f81e.up.railway.app/padre/hijos' style='background-color: " + colorCabecera + "; color: #ffffff; text-decoration: none; padding: 12px 25px; border-radius: 25px; font-weight: bold; font-size: 14px; display: inline-block;'>Ingresar al Portal del Colegio</a>" +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body></html>";
    }

    private String construirPlantillaConductaHtml(
            String tituloPrincipal,
            String saludo,
            String mensajeDetalle,
            String nombreAlumno,
            String tipoFalta,
            String tituloIncidencia,
            String fecha,
            String descripcion,
            String medidas,
            boolean citacion,
            String tutor) {

        ConfiguracionColegio colegio = configRepository.findFirstByActivoTrue().orElse(null);
        String nombreCol = colegio != null ? colegio.getNombreColegio() : "I.E.P. Internacional Jeshua";

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333;'>" +
                "  <table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background-color: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.08);'>" +
                "    <tr>" +
                "      <td style='background-color: #991b1b; padding: 25px 30px; text-align: center; color: #ffffff;'>" +
                "        <img src='https://asistencia-escolar-production-afc5f81e.up.railway.app/logo-colegio' alt='Logo' style='max-height: 60px; max-width: 120px; object-fit: contain; background: #fff; padding: 4px; border-radius: 8px;' />" +
                "        <h2 style='margin: 8px 0 0; font-size: 20px; font-weight: bold;'>" + nombreCol + "</h2>" +
                "        <p style='margin: 4px 0 0; font-size: 13px; color: #fecaca;'>Coordinación de Tutoría y Disciplina Escolar</p>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style='padding: 30px;'>" +
                "        <h3 style='margin: 0 0 15px; color: #991b1b; font-size: 18px;'>⚠️ " + tituloPrincipal + "</h3>" +
                "        <p style='margin: 0 0 15px; font-size: 15px; line-height: 1.5;'>" + saludo + "</p>" +
                "        <p style='margin: 0 0 20px; font-size: 15px; line-height: 1.5;'>" + mensajeDetalle + "</p>" +
                "        <table width='100%' style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 15px; margin-bottom: 20px; font-size: 14px;'>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Estudiante:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + nombreAlumno + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Tipo de Incidencia:</td><td style='padding: 6px 0;'><span style='background-color: #fee2e2; color: #991b1b; font-weight: bold; padding: 3px 10px; border-radius: 12px; font-size: 12px;'>" + tipoFalta + "</span></td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Motivo / Hecho:</td><td style='padding: 6px 0; font-weight: bold; color: #1e293b;'>" + tituloIncidencia + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Fecha:</td><td style='padding: 6px 0;'>" + fecha + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Tutor Responsable:</td><td style='padding: 6px 0;'>" + tutor + "</td></tr>" +
                "        </table>" +
                "        <div style='background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 15px; margin-bottom: 20px;'>" +
                "          <strong style='color: #991b1b; font-size: 13px;'>Descripción detallada de la conducta observada:</strong>" +
                "          <p style='margin: 6px 0 10px; font-size: 14px; color: #374151; line-height: 1.5;'>" + descripcion + "</p>" +
                "          <strong style='color: #991b1b; font-size: 13px;'>Medidas correctivas / Compromiso:</strong>" +
                "          <p style='margin: 6px 0 0; font-size: 14px; color: #374151; line-height: 1.5;'>" + medidas + "</p>" +
                "        </div>" +
                (citacion ? "<div style='background-color: #fff7ed; border-left: 4px solid #ea580c; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px;'><strong style='color: #c2410c;'>🚨 REQUIERE CITACIÓN PRESENCIAL:</strong><p style='margin: 4px 0 0; font-size: 14px; color: #7c2d12;'>Se solicita la presencia del apoderado en la dirección del colegio para una reunión informativa.</p></div>" : "") +
                "        <div style='text-align: center; margin-top: 25px;'>" +
                "          <a href='https://asistencia-escolar-production-afc5f81e.up.railway.app/padre/hijos' style='background-color: #991b1b; color: #ffffff; text-decoration: none; padding: 12px 25px; border-radius: 25px; font-weight: bold; font-size: 14px; display: inline-block;'>Revisar en Portal de Padres</a>" +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body></html>";
    }
}
