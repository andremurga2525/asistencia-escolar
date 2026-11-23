package com.colegio.asistencia.model;

public enum EnumEstadoJustificacion {
    PENDIENTE("Pendiente de Revisión"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada"),
    VENCIDA("Plazo de 48h Vencido");

    private final String descripcion;

    EnumEstadoJustificacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
