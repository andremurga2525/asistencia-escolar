package com.colegio.asistencia.model;

public enum EnumTurno {
    MANANA("Turno Mañana"),
    TARDE("Turno Tarde");

    private final String descripcion;

    EnumTurno(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
