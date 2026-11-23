package com.colegio.asistencia;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.ApoderadoRepository;
import com.colegio.asistencia.repository.EstudianteRepository;
import com.colegio.asistencia.repository.JustificacionInasistenciaRepository;
import com.colegio.asistencia.repository.RegistroAsistenciaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AsistenciaYJustificacionTests {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    @Autowired
    private RegistroAsistenciaRepository asistenciaRepository;

    @Autowired
    private JustificacionInasistenciaRepository justificacionRepository;

    @Test
    @DisplayName("Debe validar que el estudiante tiene un código QR aleatorio seguro (no expone DNI)")
    void testEstudianteYCodigoQr() {
        Optional<Estudiante> lucasOpt = estudianteRepository.findByDni("71234567");
        assertTrue(lucasOpt.isPresent(), "El estudiante Lucas debe existir en MySQL 8.4");

        Estudiante lucas = lucasOpt.get();
        assertNotNull(lucas.getCodigoQr(), "El código QR no debe ser nulo");
        assertFalse(lucas.getCodigoQr().contains(lucas.getDni()), "El QR NO debe exponer el DNI del estudiante");
        assertTrue(lucas.getCodigoQr().startsWith("JESHUA-"), "El QR debe pertenecer a la institución");
    }

    @Test
    @DisplayName("Debe validar que un apoderado puede tener 2 o más hijos vinculados por DNI")
    void testApoderadoConHijosVinculados() {
        Optional<Apoderado> padreOpt = apoderadoRepository.findByDni("10234567");
        assertTrue(padreOpt.isPresent(), "El apoderado debe existir");

        Apoderado padre = padreOpt.get();
        assertEquals(2, padre.getEstudiantes().size(), "El apoderado Juan Quispe debe tener 2 hijos vinculados");
        assertTrue(padre.tieneHijoConDni("71234567"), "Debe tener a su hijo mayor Lucas (DNI 71234567)");
        assertTrue(padre.tieneHijoConDni("71234568"), "Debe tener a su hijo menor Mateo (DNI 71234568)");
        assertFalse(padre.tieneHijoConDni("71234569"), "NO debe tener acceso a otros estudiantes");
    }

    @Test
    @DisplayName("Debe verificar el cálculo estricto de la ventana de 48 horas para justificaciones")
    void testPlazoEstricto48HorasJustificacion() {
        Optional<Estudiante> mateoOpt = estudianteRepository.findByDni("71234568");
        assertTrue(mateoOpt.isPresent());

        List<RegistroAsistencia> asistencias = asistenciaRepository.findByEstudianteDniOrderByFechaDesc("71234568");
        assertFalse(asistencias.isEmpty(), "Mateo debe tener registros de asistencia");

        RegistroAsistencia inasistencia = asistencias.stream()
                .filter(a -> a.getEstado() == EstadoPuntualidad.INASISTENCIA)
                .findFirst()
                .orElse(null);
        assertNotNull(inasistencia, "Debe existir un registro de inasistencia para Mateo");

        Optional<JustificacionInasistencia> justOpt = justificacionRepository.findByRegistroAsistenciaId(inasistencia.getId());
        assertTrue(justOpt.isPresent(), "Debe existir la justificación enviada por el padre");

        JustificacionInasistencia just = justOpt.get();
        
        // 1. En la fecha actual (ayer + 1 día), está DENTRO de las 48 horas
        assertTrue(just.estaDentroDelPlazo(LocalDateTime.now()), "Debe estar dentro del plazo de 48 horas");

        // 2. Si simula un momento pasado 50 horas, debe marcarse como vencido
        LocalDateTime momentoExcedido = inasistencia.getFecha().atTime(23, 59, 59).plusHours(49);
        assertFalse(just.estaDentroDelPlazo(momentoExcedido), "Debe expirar al superar las 48 horas");
    }

    @Test
    @DisplayName("Debe proporcionar estadísticas reales para el Dashboard del Administrador")
    void testEstadisticasDashboardAdmin() {
        LocalDate hoy = LocalDate.now();
        long totalRegistrosHoy = asistenciaRepository.countByFecha(hoy);
        assertTrue(totalRegistrosHoy >= 2, "Debe haber al menos 2 registros reales para el día de hoy");

        long puntualesHoy = asistenciaRepository.countByFechaAndEstado(hoy, EstadoPuntualidad.PUNTUAL);
        assertTrue(puntualesHoy >= 1, "Debe registrar al menos 1 asistencia puntual");

        long tardanzasHoy = asistenciaRepository.countByFechaAndEstado(hoy, EstadoPuntualidad.TARDANZA);
        assertTrue(tardanzasHoy >= 1, "Debe registrar al menos 1 tardanza");
    }
}
