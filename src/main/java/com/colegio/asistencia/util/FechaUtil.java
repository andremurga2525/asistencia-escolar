package com.colegio.asistencia.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Utilidad centralizada para garantizar que todas las fechas y horas del sistema
 * correspondan exactamente a la zona horaria del Perú (America/Lima, UTC-5),
 * independientemente de dónde se aloje el servidor en la nube (Railway, AWS, Docker).
 */
public final class FechaUtil {

    public static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private FechaUtil() {}

    public static LocalDate hoyPeru() {
        return LocalDate.now(ZONA_PERU);
    }

    public static LocalTime ahoraHoraPeru() {
        return LocalTime.now(ZONA_PERU);
    }

    public static LocalDateTime ahoraFechaHoraPeru() {
        return LocalDateTime.now(ZONA_PERU);
    }
}
