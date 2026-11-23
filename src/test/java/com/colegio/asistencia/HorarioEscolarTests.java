package com.colegio.asistencia;

import com.colegio.asistencia.model.*;
import com.colegio.asistencia.repository.AnioEscolarRepository;
import com.colegio.asistencia.repository.HorarioEscolarRepository;
import com.colegio.asistencia.repository.NivelEducativoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HorarioEscolarTests {

    @Autowired
    private AnioEscolarRepository anioEscolarRepository;

    @Autowired
    private NivelEducativoRepository nivelRepository;

    @Autowired
    private HorarioEscolarRepository horarioRepository;

    @Test
    @DisplayName("Debe validar que el año escolar 2026 está activo en MySQL 8.4")
    void testAnioEscolarActivo() {
        Optional<AnioEscolar> anioOpt = anioEscolarRepository.findFirstByActivoTrue();
        assertTrue(anioOpt.isPresent(), "Debe existir un año escolar activo");
        assertEquals(2026, anioOpt.get().getAnio());
    }

    @Test
    @DisplayName("Debe validar que existen los 3 niveles educativos en MySQL 8.4")
    void testNivelesEducativos() {
        assertTrue(nivelRepository.findByCodigo(EnumNivel.INICIAL).isPresent(), "Nivel Inicial debe existir");
        assertTrue(nivelRepository.findByCodigo(EnumNivel.PRIMARIA).isPresent(), "Nivel Primaria debe existir");
        assertTrue(nivelRepository.findByCodigo(EnumNivel.SECUNDARIA).isPresent(), "Nivel Secundaria debe existir");
    }

    @Test
    @DisplayName("Debe evaluar la puntualidad con las reglas oficiales (Ejemplo: Primaria 07:30 + 10m tol)")
    void testReglaCalculoPuntualidadPrimaria() {
        Optional<HorarioEscolar> horarioOpt = horarioRepository
                .findByNivelCodigoAndTurnoCodigoAndActivoTrue(EnumNivel.PRIMARIA, EnumTurno.MANANA);
        
        assertTrue(horarioOpt.isPresent(), "Debe existir horario de Primaria Mañana");
        HorarioEscolar horario = horarioOpt.get();

        // 1. Llegada antes de la entrada (07:15 < 07:30) -> TEMPRANO
        assertEquals(EstadoPuntualidad.TEMPRANO, horario.evaluarEntrada(LocalTime.of(7, 15)));

        // 2. Llegada a la hora exacta de entrada (07:30) -> PUNTUAL
        assertEquals(EstadoPuntualidad.PUNTUAL, horario.evaluarEntrada(LocalTime.of(7, 30)));

        // 3. Llegada en el minuto exacto del límite de tolerancia (07:40) -> PUNTUAL
        assertEquals(EstadoPuntualidad.PUNTUAL, horario.evaluarEntrada(LocalTime.of(7, 40)));

        // 4. Llegada un minuto después de la tolerancia (07:41) -> TARDANZA
        assertEquals(EstadoPuntualidad.TARDANZA, horario.evaluarEntrada(LocalTime.of(7, 41)));

        // 5. Llegada pasada la hora límite de tardanza (08:01 > 08:00) -> INASISTENCIA
        assertEquals(EstadoPuntualidad.INASISTENCIA, horario.evaluarEntrada(LocalTime.of(8, 1)));
    }
}
