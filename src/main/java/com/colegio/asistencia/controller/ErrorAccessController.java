package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorAccessController {

    private final ConfiguracionColegioRepository configuracionRepository;

    public ErrorAccessController(ConfiguracionColegioRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        ConfiguracionColegio colegio = configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("I.E.P. Internacional Jeshua", "Lema", "-", "-", "-"));
        model.addAttribute("colegio", colegio);
        return "error/403";
    }
}
