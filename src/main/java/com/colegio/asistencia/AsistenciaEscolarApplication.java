package com.colegio.asistencia;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class AsistenciaEscolarApplication {

    @PostConstruct
    public void init() {
        // Fijar la zona horaria oficial del Perú (America/Lima, UTC-5) para todo el servidor y contenedores en la nube
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        System.setProperty("user.timezone", "America/Lima");
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        System.setProperty("user.timezone", "America/Lima");
        SpringApplication.run(AsistenciaEscolarApplication.class, args);
    }
}
