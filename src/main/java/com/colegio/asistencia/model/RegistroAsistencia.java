package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "registros_asistencia", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"estudiante_id", "fecha"})
})
public class RegistroAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El estudiante es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @NotNull(message = "El año escolar es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "anio_escolar_id", nullable = false)
    private AnioEscolar anioEscolar;

    @NotNull(message = "La matrícula es obligatoria")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @NotNull(message = "La fecha es obligatoria")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    @NotNull(message = "El estado de asistencia es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPuntualidad estado;

    @Column(name = "minutos_tardanza")
    private Integer minutosTardanza;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "auxiliar_id")
    private Usuario auxiliarRegistro;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public RegistroAsistencia() {
        this.fechaRegistro = LocalDateTime.now();
        this.minutosTardanza = 0;
    }

    public RegistroAsistencia(Estudiante estudiante, AnioEscolar anioEscolar, Matricula matricula,
                              LocalDate fecha, LocalTime horaEntrada, EstadoPuntualidad estado, Usuario auxiliar) {
        this();
        this.estudiante = estudiante;
        this.anioEscolar = anioEscolar;
        this.matricula = matricula;
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.estado = estado;
        this.auxiliarRegistro = auxiliar;
    }

    // Getters y Setters completos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante e) { this.estudiante = e; }
    public AnioEscolar getAnioEscolar() { return anioEscolar; }
    public void setAnioEscolar(AnioEscolar a) { this.anioEscolar = a; }
    public Matricula getMatricula() { return matricula; }
    public void setMatricula(Matricula m) { this.matricula = m; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate f) { this.fecha = f; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime h) { this.horaEntrada = h; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime h) { this.horaSalida = h; }
    public EstadoPuntualidad getEstado() { return estado; }
    public void setEstado(EstadoPuntualidad e) { this.estado = e; }
    public Integer getMinutosTardanza() { return minutosTardanza; }
    public void setMinutosTardanza(Integer m) { this.minutosTardanza = m; }
    public Usuario getAuxiliarRegistro() { return auxiliarRegistro; }
    public void setAuxiliarRegistro(Usuario a) { this.auxiliarRegistro = a; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String o) { this.observaciones = o; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime f) { this.fechaRegistro = f; }
}
