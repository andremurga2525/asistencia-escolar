package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "comunicados_aula")
public class ComunicadoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título del comunicado es obligatorio")
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @NotBlank(message = "El contenido del comunicado es obligatorio")
    @Column(name = "mensaje", columnDefinition = "TEXT", nullable = false)
    private String mensaje;

    @Column(name = "prioridad", length = 30, nullable = false)
    private String prioridad; // URGENTE, NORMAL, INFORMATIVO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grado_id")
    private Grado grado; // Nullable para comunicados a todo el colegio

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id")
    private Seccion seccion; // Nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Usuario tutor;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "archivo_adjunto_nombre", length = 200)
    private String archivoAdjuntoNombre;

    @Lob
    @Column(name = "archivo_adjunto_data", columnDefinition = "LONGBLOB")
    private byte[] archivoAdjuntoData;

    @Column(name = "archivo_adjunto_content_type", length = 100)
    private String archivoAdjuntoContentType;

    @Column(name = "notificado_email")
    private Boolean notificadoEmail;

    public ComunicadoAula() {
        this.prioridad = "NORMAL";
        this.notificadoEmail = false;
        this.fechaPublicacion = LocalDateTime.now();
    }

    public ComunicadoAula(String titulo, String mensaje, String prioridad, Grado grado, Seccion seccion, Usuario tutor) {
        this();
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.prioridad = prioridad;
        this.grado = grado;
        this.seccion = seccion;
        this.tutor = tutor;
    }

    public boolean isUrgente() {
        return "URGENTE".equalsIgnoreCase(prioridad);
    }

    public boolean tieneArchivo() {
        return archivoAdjuntoData != null && archivoAdjuntoData.length > 0;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public Grado getGrado() {
        return grado;
    }

    public void setGrado(Grado grado) {
        this.grado = grado;
    }

    public Seccion getSeccion() {
        return seccion;
    }

    public void setSeccion(Seccion seccion) {
        this.seccion = seccion;
    }

    public Usuario getTutor() {
        return tutor;
    }

    public void setTutor(Usuario tutor) {
        this.tutor = tutor;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public String getArchivoAdjuntoNombre() {
        return archivoAdjuntoNombre;
    }

    public void setArchivoAdjuntoNombre(String archivoAdjuntoNombre) {
        this.archivoAdjuntoNombre = archivoAdjuntoNombre;
    }

    public byte[] getArchivoAdjuntoData() {
        return archivoAdjuntoData;
    }

    public void setArchivoAdjuntoData(byte[] archivoAdjuntoData) {
        this.archivoAdjuntoData = archivoAdjuntoData;
    }

    public String getArchivoAdjuntoContentType() {
        return archivoAdjuntoContentType;
    }

    public void setArchivoAdjuntoContentType(String archivoAdjuntoContentType) {
        this.archivoAdjuntoContentType = archivoAdjuntoContentType;
    }

    public Boolean getNotificadoEmail() {
        return notificadoEmail;
    }

    public void setNotificadoEmail(Boolean notificadoEmail) {
        this.notificadoEmail = notificadoEmail;
    }
}
