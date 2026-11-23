package com.colegio.asistencia.model;

import java.util.ArrayList;
import java.util.List;

public class ResultadoImportacion {

    private int totalProcesados = 0;
    private int totalCreados = 0;
    private int totalActualizados = 0;
    private int totalMatriculados = 0;
    private int totalApoderadosVinculados = 0;
    private List<String> errores = new ArrayList<>();
    private List<String> exitosos = new ArrayList<>();

    public void incrementarProcesados() { this.totalProcesados++; }
    public void incrementarCreados() { this.totalCreados++; }
    public void incrementarActualizados() { this.totalActualizados++; }
    public void incrementarMatriculados() { this.totalMatriculados++; }
    public void incrementarApoderadosVinculados() { this.totalApoderadosVinculados++; }

    public void agregarError(String error) { this.errores.add(error); }
    public void agregarExitoso(String msg) { this.exitosos.add(msg); }

    public int getTotalProcesados() { return totalProcesados; }
    public void setTotalProcesados(int totalProcesados) { this.totalProcesados = totalProcesados; }

    public int getTotalCreados() { return totalCreados; }
    public void setTotalCreados(int totalCreados) { this.totalCreados = totalCreados; }

    public int getTotalActualizados() { return totalActualizados; }
    public void setTotalActualizados(int totalActualizados) { this.totalActualizados = totalActualizados; }

    public int getTotalMatriculados() { return totalMatriculados; }
    public void setTotalMatriculados(int totalMatriculados) { this.totalMatriculados = totalMatriculados; }

    public int getTotalApoderadosVinculados() { return totalApoderadosVinculados; }
    public void setTotalApoderadosVinculados(int totalApoderadosVinculados) { this.totalApoderadosVinculados = totalApoderadosVinculados; }

    public List<String> getErrores() { return errores; }
    public void setErrores(List<String> errores) { this.errores = errores; }

    public List<String> getExitosos() { return exitosos; }
    public void setExitosos(List<String> exitosos) { this.exitosos = exitosos; }
}
