package com.colegio.asistencia.model;

public enum EstadoPuntualidad {
    TEMPRANO("Llegada Anticipada"),
    PUNTUAL("Puntual"),
    TARDANZA("Tardanza"),
    INASISTENCIA("Inasistencia"),
    JUSTIFICADO("Inasistencia Justificada");

    private final String descripcion;

    EstadoPuntualidad(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
