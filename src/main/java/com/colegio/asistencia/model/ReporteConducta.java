package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reportes_conducta")
public class ReporteConducta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Debe seleccionar el estudiante")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Usuario tutor;

    @Column(name = "fecha_incidencia", nullable = false)
    private LocalDate fechaIncidencia;

    @Column(name = "hora_incidencia")
    private LocalTime horaIncidencia;

    @NotBlank(message = "El tipo de falta / reporte es obligatorio")
    @Column(name = "tipo_falta", length = 50, nullable = false)
    private String tipoFalta; // FALTA_LEVE, FALTA_GRAVE, LLAMADA_ATENCION, CITACION_APODERADO, FELICITACION

    @NotBlank(message = "El título o motivo es obligatorio")
    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(name = "medidas_tomadas", columnDefinition = "TEXT")
    private String medidasTomadas;

    @Column(name = "requiere_citacion")
    private Boolean requiereCitacion;

    @Column(name = "fecha_citacion")
    private LocalDateTime fechaCitacion;

    @Column(name = "notificado_padre")
    private Boolean notificadoPadre;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public ReporteConducta() {
        this.requiereCitacion = false;
        this.notificadoPadre = false;
        this.fechaIncidencia = LocalDate.now();
        this.horaIncidencia = LocalTime.now();
        this.fechaCreacion = LocalDateTime.now();
    }

    public ReporteConducta(Estudiante estudiante, Usuario tutor, String tipoFalta, String titulo, String descripcion) {
        this();
        this.estudiante = estudiante;
        this.tutor = tutor;
        this.tipoFalta = tipoFalta;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public boolean isGrave() {
        return "FALTA_GRAVE".equalsIgnoreCase(tipoFalta) || "CITACION_APODERADO".equalsIgnoreCase(tipoFalta);
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Usuario getTutor() {
        return tutor;
    }

    public void setTutor(Usuario tutor) {
        this.tutor = tutor;
    }

    public LocalDate getFechaIncidencia() {
        return fechaIncidencia;
    }

    public void setFechaIncidencia(LocalDate fechaIncidencia) {
        this.fechaIncidencia = fechaIncidencia;
    }

    public LocalTime getHoraIncidencia() {
        return horaIncidencia;
    }

    public void setHoraIncidencia(LocalTime horaIncidencia) {
        this.horaIncidencia = horaIncidencia;
    }

    public String getTipoFalta() {
        return tipoFalta;
    }

    public void setTipoFalta(String tipoFalta) {
        this.tipoFalta = tipoFalta;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMedidasTomadas() {
        return medidasTomadas;
    }

    public void setMedidasTomadas(String medidasTomadas) {
        this.medidasTomadas = medidasTomadas;
    }

    public Boolean getRequiereCitacion() {
        return requiereCitacion;
    }

    public void setRequiereCitacion(Boolean requiereCitacion) {
        this.requiereCitacion = requiereCitacion;
    }

    public LocalDateTime getFechaCitacion() {
        return fechaCitacion;
    }

    public void setFechaCitacion(LocalDateTime fechaCitacion) {
        this.fechaCitacion = fechaCitacion;
    }

    public Boolean getNotificadoPadre() {
        return notificadoPadre;
    }

    public void setNotificadoPadre(Boolean notificadoPadre) {
        this.notificadoPadre = notificadoPadre;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
