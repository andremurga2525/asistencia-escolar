package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_escolares")
public class HorarioEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del horario es obligatorio")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotNull(message = "El nivel educativo es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelEducativo nivel;

    @NotNull(message = "El turno es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @NotNull(message = "La hora de apertura del escáner es obligatoria")
    @Column(name = "hora_apertura_registro", nullable = false)
    private LocalTime horaAperturaRegistro;

    @NotNull(message = "La hora oficial de entrada es obligatoria")
    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @NotNull(message = "Los minutos de tolerancia son obligatorios")
    @Min(value = 0, message = "La tolerancia no puede ser negativa")
    @Column(name = "tolerancia_minutos", nullable = false)
    private Integer toleranciaMinutos;

    @NotNull(message = "La hora límite de tardanza es obligatoria")
    @Column(name = "hora_limite_tardanza", nullable = false)
    private LocalTime horaLimiteTardanza;

    @NotNull(message = "La hora de inicio de salida es obligatoria")
    @Column(name = "hora_inicio_salida", nullable = false)
    private LocalTime horaInicioSalida;

    @NotNull(message = "La hora oficial de salida es obligatoria")
    @Column(name = "hora_salida", nullable = false)
    private LocalTime horaSalida;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public HorarioEscolar() {
        this.activo = true;
        this.toleranciaMinutos = 10;
    }

    public HorarioEscolar(String nombre, NivelEducativo nivel, Turno turno,
                          LocalTime horaAperturaRegistro, LocalTime horaEntrada,
                          Integer toleranciaMinutos, LocalTime horaLimiteTardanza,
                          LocalTime horaInicioSalida, LocalTime horaSalida) {
        this();
        this.nombre = nombre;
        this.nivel = nivel;
        this.turno = turno;
        this.horaAperturaRegistro = horaAperturaRegistro;
        this.horaEntrada = horaEntrada;
        this.toleranciaMinutos = toleranciaMinutos;
        this.horaLimiteTardanza = horaLimiteTardanza;
        this.horaInicioSalida = horaInicioSalida;
        this.horaSalida = horaSalida;
    }

    /**
     * Calcula el límite máximo de puntualidad (hora de entrada + minutos de tolerancia)
     */
    public LocalTime getLimitePuntualidad() {
        return this.horaEntrada.plusMinutes(this.toleranciaMinutos);
    }

    /**
     * Evalúa la puntualidad según las reglas oficiales de asistencia escolar:
     * - Antes de la hora de entrada: TEMPRANO (llegada anticipada)
     * - Hasta horaEntrada + tolerancia (inclusive): PUNTUAL
     * - Después de la tolerancia y hasta horaLimiteTardanza: TARDANZA
     * - Pasada la hora límite: INASISTENCIA (cierre de puerta)
     */
    public EstadoPuntualidad evaluarEntrada(LocalTime horaLlegada) {
        if (horaLlegada.isBefore(this.horaEntrada)) {
            return EstadoPuntualidad.TEMPRANO;
        }

        LocalTime limitePuntualidad = getLimitePuntualidad();
        if (!horaLlegada.isAfter(limitePuntualidad)) {
            return EstadoPuntualidad.PUNTUAL;
        }

        if (!horaLlegada.isAfter(this.horaLimiteTardanza)) {
            return EstadoPuntualidad.TARDANZA;
        }

        return EstadoPuntualidad.INASISTENCIA;
    }

    // Getters y Setters completos
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public NivelEducativo getNivel() {
        return nivel;
    }

    public void setNivel(NivelEducativo nivel) {
        this.nivel = nivel;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public LocalTime getHoraAperturaRegistro() {
        return horaAperturaRegistro;
    }

    public void setHoraAperturaRegistro(LocalTime horaAperturaRegistro) {
        this.horaAperturaRegistro = horaAperturaRegistro;
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(LocalTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public Integer getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public void setToleranciaMinutos(Integer toleranciaMinutos) {
        this.toleranciaMinutos = toleranciaMinutos;
    }

    public LocalTime getHoraLimiteTardanza() {
        return horaLimiteTardanza;
    }

    public void setHoraLimiteTardanza(LocalTime horaLimiteTardanza) {
        this.horaLimiteTardanza = horaLimiteTardanza;
    }

    public LocalTime getHoraInicioSalida() {
        return horaInicioSalida;
    }

    public void setHoraInicioSalida(LocalTime horaInicioSalida) {
        this.horaInicioSalida = horaInicioSalida;
    }

    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(LocalTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
