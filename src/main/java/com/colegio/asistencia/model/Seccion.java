package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "secciones")
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la sección es obligatorio (ej: A, B, Única)")
    @Column(name = "nombre", nullable = false, length = 20)
    private String nombre;

    @Column(name = "aula", length = 50)
    private String aula;

    @NotNull(message = "El grado es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Seccion() {
        this.activo = true;
    }

    public Seccion(String nombre, String aula, Grado grado) {
        this();
        this.nombre = nombre;
        this.aula = aula;
        this.grado = grado;
    }

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

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    public Grado getGrado() {
        return grado;
    }

    public void setGrado(Grado grado) {
        this.grado = grado;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
