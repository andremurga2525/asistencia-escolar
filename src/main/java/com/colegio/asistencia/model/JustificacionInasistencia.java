package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "justificaciones_inasistencia")
public class JustificacionInasistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El registro de asistencia es obligatorio")
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "registro_asistencia_id", nullable = false, unique = true)
    private RegistroAsistencia registroAsistencia;

    @NotNull(message = "El apoderado solicitante es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Apoderado apoderado;

    @NotBlank(message = "El motivo de la justificación es obligatorio")
    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "archivo_evidencia", length = 255)
    private String archivoEvidencia;

    @NotNull
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    /**
     * Plazo límite estricto de 48 horas calculado por el servidor (America/Lima).
     */
    @NotNull
    @Column(name = "fecha_limite_48h", nullable = false)
    private LocalDateTime fechaLimite48h;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EnumEstadoJustificacion estado;

    @Column(name = "observacion_revision", length = 300)
    private String observacionRevision;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "revisado_por_usuario_id")
    private Usuario revisadoPor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    public JustificacionInasistencia() {
        this.fechaSolicitud = LocalDateTime.now();
        this.estado = EnumEstadoJustificacion.PENDIENTE;
    }

    public JustificacionInasistencia(RegistroAsistencia registroAsistencia, Apoderado apoderado, String motivo, String archivoEvidencia) {
        this();
        this.registroAsistencia = registroAsistencia;
        this.apoderado = apoderado;
        this.motivo = motivo;
        this.archivoEvidencia = archivoEvidencia;
        // El plazo de 48 horas se calcula desde el final del día de la inasistencia
        this.fechaLimite48h = registroAsistencia.getFecha().atTime(23, 59, 59).plusHours(48);
    }

    /**
     * Valida si la solicitud se encuentra dentro del plazo legal de 48 horas.
     */
    public boolean estaDentroDelPlazo(LocalDateTime momento) {
        return !momento.isAfter(this.fechaLimite48h);
    }

    // Getters y Setters completos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RegistroAsistencia getRegistroAsistencia() { return registroAsistencia; }
    public void setRegistroAsistencia(RegistroAsistencia r) { this.registroAsistencia = r; }
    public Apoderado getApoderado() { return apoderado; }
    public void setApoderado(Apoderado a) { this.apoderado = a; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String m) { this.motivo = m; }
    public String getArchivoEvidencia() { return archivoEvidencia; }
    public void setArchivoEvidencia(String a) { this.archivoEvidencia = a; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime f) { this.fechaSolicitud = f; }
    public LocalDateTime getFechaLimite48h() { return fechaLimite48h; }
    public void setFechaLimite48h(LocalDateTime f) { this.fechaLimite48h = f; }
    public EnumEstadoJustificacion getEstado() { return estado; }
    public void setEstado(EnumEstadoJustificacion e) { this.estado = e; }
    public String getObservacionRevision() { return observacionRevision; }
    public void setObservacionRevision(String o) { this.observacionRevision = o; }
    public Usuario getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(Usuario u) { this.revisadoPor = u; }
    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime f) { this.fechaRevision = f; }
}
