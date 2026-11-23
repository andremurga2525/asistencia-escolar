package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "matriculas", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"estudiante_id", "anio_escolar_id"})
})
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código de matrícula es obligatorio")
    @Column(name = "codigo_matricula", nullable = false, unique = true, length = 30)
    private String codigoMatricula;

    @NotNull(message = "El estudiante es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @NotNull(message = "El año escolar es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "anio_escolar_id", nullable = false)
    private AnioEscolar anioEscolar;

    @NotNull(message = "La sección es obligatoria")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @NotNull(message = "El turno es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @NotNull
    @Column(name = "fecha_matricula", nullable = false)
    private LocalDate fechaMatricula;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Matricula() {
        this.activo = true;
        this.fechaMatricula = LocalDate.now();
    }

    public Matricula(String codigoMatricula, Estudiante estudiante, AnioEscolar anioEscolar, Seccion seccion, Turno turno) {
        this();
        this.codigoMatricula = codigoMatricula;
        this.estudiante = estudiante;
        this.anioEscolar = anioEscolar;
        this.seccion = seccion;
        this.turno = turno;
    }

    // Getters y Setters completos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoMatricula() { return codigoMatricula; }
    public void setCodigoMatricula(String c) { this.codigoMatricula = c; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante e) { this.estudiante = e; }
    public AnioEscolar getAnioEscolar() { return anioEscolar; }
    public void setAnioEscolar(AnioEscolar a) { this.anioEscolar = a; }
    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion s) { this.seccion = s; }
    public Turno getTurno() { return turno; }
    public void setTurno(Turno t) { this.turno = t; }
    public LocalDate getFechaMatricula() { return fechaMatricula; }
    public void setFechaMatricula(LocalDate f) { this.fechaMatricula = f; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
