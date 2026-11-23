package com.colegio.asistencia.model;

public enum EnumRol {
    ROLE_ADMIN("Administrador", "Acceso total a la configuración y gestión del sistema"),
    ROLE_AUXILIAR("Auxiliar de Asistencia", "Gestión de asistencias, tardanzas y justificaciones"),
    ROLE_PADRE("Padre de Familia / Apoderado", "Consulta de asistencia y envío de justificaciones de sus hijos"),
    ROLE_ESTUDIANTE("Estudiante", "Consulta de asistencias y fotocheck digital");

    private final String nombreLegible;
    private final String descripcion;

    EnumRol(String nombreLegible, String descripcion) {
        this.nombreLegible = nombreLegible;
        this.descripcion = descripcion;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
