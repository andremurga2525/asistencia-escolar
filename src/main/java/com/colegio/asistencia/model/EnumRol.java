package com.colegio.asistencia.model;

public enum EnumRol {
    ROLE_ADMIN("Administrador", "Acceso total a la configuración y gestión del sistema"),
    ROLE_AUXILIAR("Auxiliar de Asistencia", "Gestión de asistencias, tardanzas y justificaciones en puerta"),
    ROLE_PROFESOR("Profesor", "Gestión de tareas escolares, materiales y seguimiento pedagógico"),
    ROLE_TUTOR("Tutor de Aula", "Gestión de comunicados urgentes a padres, citaciones y reportes de conducta"),
    ROLE_PADRE("Padre / Madre / Apoderado", "Portal familiar: consulta de asistencia, tareas, comunicados y justificaciones"),
    ROLE_ESTUDIANTE("Estudiante", "Consulta de asistencias, fotocheck digital y tareas escolares");

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
