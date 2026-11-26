package com.colegio.asistencia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AsistenciaEscolarApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsistenciaEscolarApplication.class, args);
    }
}
