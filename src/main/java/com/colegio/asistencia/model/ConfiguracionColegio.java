package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_colegio")
public class ConfiguracionColegio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del colegio no puede estar vacío")
    @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
    @Column(name = "nombre_colegio", nullable = false, length = 150)
    private String nombreColegio;

    @Size(max = 255, message = "El lema no puede exceder los 255 caracteres")
    @Column(name = "lema", length = 255)
    private String lema;

    @Size(max = 200, message = "La dirección no puede exceder los 200 caracteres")
    @Column(name = "direccion", length = 200)
    private String direccion;

    @Size(max = 30, message = "El teléfono no puede exceder los 30 caracteres")
    @Column(name = "telefono", length = 30)
    private String telefono;

    @Size(max = 100, message = "El correo no puede exceder los 100 caracteres")
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Lob
    @Column(name = "logo_data", columnDefinition = "LONGBLOB")
    private byte[] logoData;

    @Column(name = "logo_mime_type", length = 50)
    private String logoMimeType;

    @Column(name = "color_primario", length = 50)
    private String colorPrimario;

    @Column(name = "color_secundario", length = 50)
    private String colorSecundario;

    @Column(name = "email_notificaciones", length = 100)
    private String emailNotificaciones;

    @Column(name = "email_password", length = 100)
    private String emailPassword;

    @Column(name = "email_activo")
    private Boolean emailActivo = true;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public ConfiguracionColegio() {
        this.activo = true;
        this.colorPrimario = "#b91c1c";
        this.colorSecundario = "#ffffff";
        this.emailActivo = true;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public ConfiguracionColegio(String nombreColegio, String lema, String direccion, String telefono, String email) {
        this();
        this.nombreColegio = nombreColegio;
        this.lema = lema;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters completos
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreColegio() {
        return nombreColegio;
    }

    public void setNombreColegio(String nombreColegio) {
        this.nombreColegio = nombreColegio;
    }

    public String getLema() {
        return lema;
    }

    public void setLema(String lema) {
        this.lema = lema;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public byte[] getLogoData() {
        return logoData;
    }

    public void setLogoData(byte[] logoData) {
        this.logoData = logoData;
    }

    public String getLogoMimeType() {
        return logoMimeType;
    }

    public void setLogoMimeType(String logoMimeType) {
        this.logoMimeType = logoMimeType;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getEmailNotificaciones() {
        return emailNotificaciones;
    }

    public void setEmailNotificaciones(String emailNotificaciones) {
        this.emailNotificaciones = emailNotificaciones;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public void setEmailPassword(String emailPassword) {
        this.emailPassword = emailPassword;
    }

    public Boolean getEmailActivo() {
        return emailActivo;
    }

    public void setEmailActivo(Boolean emailActivo) {
        this.emailActivo = emailActivo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
