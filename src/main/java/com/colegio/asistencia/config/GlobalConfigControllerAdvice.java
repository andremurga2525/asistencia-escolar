package com.colegio.asistencia.config;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice global para garantizar que todos los controladores y plantillas Thymeleaf
 * tengan disponible automáticamente la información del colegio (${colegio} y ${configColegio}),
 * incluyendo su nombre oficial, lema, colores distintivos y logotipo en todos los portales y vistas.
 */
@ControllerAdvice
public class GlobalConfigControllerAdvice {

    private final ConfiguracionColegioRepository configRepository;

    public GlobalConfigControllerAdvice(ConfiguracionColegioRepository configRepository) {
        this.configRepository = configRepository;
    }

    @ModelAttribute("colegio")
    public ConfiguracionColegio agregarColegioGlobal() {
        return configRepository.findFirstByActivoTrue().orElseGet(() -> new ConfiguracionColegio(
                "I.E.P. Internacional Jeshua",
                "Instruye al niño en su camino, y aun cuando fuere viejo no se apartará de él",
                "Tumán, Chiclayo, Lambayeque - Perú",
                "+51 974 123 456",
                "contacto@internacionaljeshua.edu.pe"
        ));
    }

    @ModelAttribute("configColegio")
    public ConfiguracionColegio agregarConfigColegioGlobal() {
        return agregarColegioGlobal();
    }
}
