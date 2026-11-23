package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "grados")
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del grado es obligatorio")
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @NotNull(message = "El número o nivel del grado es obligatorio")
    @Column(name = "numero", nullable = false)
    private Integer numero;

    @NotNull(message = "El nivel educativo es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelEducativo nivel;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Grado() {
        this.activo = true;
    }

    public Grado(String nombre, Integer numero, NivelEducativo nivel) {
        this();
        this.nombre = nombre;
        this.numero = numero;
        this.nivel = nivel;
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

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public NivelEducativo getNivel() {
        return nivel;
    }

    public void setNivel(NivelEducativo nivel) {
        this.nivel = nivel;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
