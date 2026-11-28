package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tareas_escolares")
public class TareaEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título de la tarea es obligatorio")
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @NotBlank(message = "El curso / materia es obligatorio")
    @Column(name = "materia", nullable = false, length = 100)
    private String materia;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @NotNull(message = "Debe asignar el grado escolar")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @NotNull(message = "Debe asignar la sección")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Usuario profesor;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @NotNull(message = "La fecha límite de entrega es obligatoria")
    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @Column(name = "archivo_adjunto_nombre", length = 200)
    private String archivoAdjuntoNombre;

    @Lob
    @Column(name = "archivo_adjunto_data", columnDefinition = "LONGBLOB")
    private byte[] archivoAdjuntoData;

    @Column(name = "archivo_adjunto_content_type", length = 100)
    private String archivoAdjuntoContentType;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public TareaEscolar() {
        this.activo = true;
        this.fechaAsignacion = LocalDate.now();
        this.fechaCreacion = LocalDateTime.now();
    }

    public TareaEscolar(String titulo, String materia, String descripcion, Grado grado, Seccion seccion, Usuario profesor, LocalDate fechaEntrega) {
        this();
        this.titulo = titulo;
        this.materia = materia;
        this.descripcion = descripcion;
        this.grado = grado;
        this.seccion = seccion;
        this.profesor = profesor;
        this.fechaEntrega = fechaEntrega;
    }

    public boolean isVencida() {
        return fechaEntrega != null && fechaEntrega.isBefore(LocalDate.now());
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

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public Usuario getProfesor() {
        return profesor;
    }

    public void setProfesor(Usuario profesor) {
        this.profesor = profesor;
    }

    public LocalDate getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDate fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
