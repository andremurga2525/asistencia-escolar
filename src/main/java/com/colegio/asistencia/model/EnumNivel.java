package com.colegio.asistencia.model;

public enum EnumNivel {
    INICIAL("Nivel Inicial", 1),
    PRIMARIA("Nivel Primaria", 2),
    SECUNDARIA("Nivel Secundaria", 3);

    private final String descripcion;
    private final int orden;

    EnumNivel(String descripcion, int orden) {
        this.descripcion = descripcion;
        this.orden = orden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getOrden() {
        return orden;
    }
}
