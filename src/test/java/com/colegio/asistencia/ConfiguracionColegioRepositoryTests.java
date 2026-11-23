package com.colegio.asistencia;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConfiguracionColegioRepositoryTests {

    @Autowired
    private ConfiguracionColegioRepository repository;

    @Test
    @DisplayName("Debe conectar a MySQL y recuperar o guardar la configuración del colegio")
    void testConexionYConfiguracionColegio() {
        assertNotNull(repository, "El repositorio debe haberse inyectado correctamente");

        Optional<ConfiguracionColegio> configOptional = repository.findFirstByActivoTrue();
        assertTrue(configOptional.isPresent(), "Debe existir al menos una configuración activa creada por el seeder");

        ConfiguracionColegio config = configOptional.get();
        assertNotNull(config.getId(), "El ID no debe ser nulo");
        assertNotNull(config.getNombreColegio(), "El nombre del colegio no debe ser nulo");
        assertTrue(config.getActivo(), "La configuración debe estar activa");
    }
}
