package com.colegio.asistencia.service;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.ApoderadoRepository;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificacionService.class);

    private final JavaMailSender mailSender;
    private final ApoderadoRepository apoderadoRepository;
    private final ConfiguracionColegioRepository configRepository;

    @Value("${spring.mail.username:notificaciones.jeshua@gmail.com}")
    private String remitente;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailNotificacionService(
            @Autowired(required = false) JavaMailSender mailSender,
            ApoderadoRepository apoderadoRepository,
            ConfiguracionColegioRepository configRepository) {
        this.mailSender = mailSender;
        this.apoderadoRepository = apoderadoRepository;
        this.configRepository = configRepository;
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

        // Si no hay configuración activa de contraseña SMTP (por ejemplo en ambiente de desarrollo local),
        // se registra la simulación en los logs para auditoría sin detener la operación.
        if (mailSender == null || mailPassword == null || mailPassword.trim().isEmpty()) {
            log.info("ℹ️ Servicio SMTP en modo Simulación/Desarrollo (Configure SPRING_MAIL_PASSWORD para envío real a Gmail).");
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "I.E.P. Internacional Jeshua");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            mailSender.send(mensaje);
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

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333;'>" +
                "  <table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.08);'>" +
                "    <tr>" +
                "      <td style='background-color: #b91c1c; padding: 25px 30px; text-align: center; color: #ffffff;'>" +
                "        <h2 style='margin: 0; font-size: 22px; font-weight: bold; letter-spacing: 0.5px;'>" + nombreCol + "</h2>" +
                "        <p style='margin: 5px 0 0; font-size: 13px; color: #fecaca;'>Sistema Integrado de Asistencia y Trazabilidad Escolar</p>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style='padding: 30px;'>" +
                "        <h3 style='margin: 0 0 15px; color: #111827; font-size: 18px;'>" + tituloPrincipal + "</h3>" +
                "        <p style='margin: 0 0 15px; font-size: 15px; line-height: 1.5;'>" + saludo + "</p>" +
                "        <p style='margin: 0 0 20px; font-size: 15px; line-height: 1.5;'>" + mensajeDetalle + "</p>" +
                "        <table width='100%' style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; margin-bottom: 20px; font-size: 14px;'>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Estudiante:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + nombreAlumno + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Fecha:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + fecha + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Hora registrada:</td><td style='padding: 6px 0; font-weight: bold; color: #0f172a;'>" + hora + "</td></tr>" +
                "          <tr><td style='padding: 6px 0; color: #64748b;'>Estado:</td><td style='padding: 6px 0;'><span style='background-color: " + colorEstado + "; color: #ffffff; font-weight: bold; padding: 4px 10px; border-radius: 20px; font-size: 12px;'>" + estadoTexto + "</span></td></tr>" +
                "        </table>" +
                "        <p style='margin: 0 0 25px; font-size: 13px; color: #64748b; font-style: italic;'>" + notaPie + "</p>" +
                "        <div style='text-align: center; margin-bottom: 10px;'>" +
                "          <a href='https://asistencia-escolar-production-afc5f81e.up.railway.app/padre/hijos' style='background-color: #b91c1c; color: #ffffff; text-decoration: none; padding: 12px 25px; border-radius: 25px; font-weight: bold; font-size: 14px; display: inline-block;'>Abrir Portal de Padres</a>" +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style='background-color: #f1f5f9; padding: 15px 30px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;'>" +
                "        Este es un mensaje automático de control escolar emitido por I.E.P. Internacional Jeshua (Tumán - Chiclayo)." +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }
}
