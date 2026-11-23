package com.colegio.asistencia.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private EnumRol nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    public Rol() {
    }

    public Rol(EnumRol nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EnumRol getNombre() {
        return nombre;
    }

    public void setNombre(EnumRol nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rol rol = (Rol) o;
        return Objects.equals(id, rol.id) && nombre == rol.nombre;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre);
    }
}
