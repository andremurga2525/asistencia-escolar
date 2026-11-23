package com.colegio.asistencia.controller;

import com.colegio.asistencia.model.ConfiguracionColegio;
import com.colegio.asistencia.repository.ConfiguracionColegioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HealthCheckController {

    private final ConfiguracionColegioRepository configuracionRepository;

    public HealthCheckController(ConfiguracionColegioRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @GetMapping("/api/health")
    @ResponseBody
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("app", "Sistema de Asistencia Escolar");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Verificación de conexión a MySQL
        try {
            long totalConfig = configuracionRepository.count();
            ConfiguracionColegio config = configuracionRepository.findFirstByActivoTrue().orElse(null);
            response.put("database", Map.of(
                    "status", "CONNECTED",
                    "colegio", config != null ? config.getNombreColegio() : "No registrado",
                    "totalRegistros", totalConfig
            ));
        } catch (Exception e) {
            response.put("database", Map.of(
                    "status", "DISCONNECTED",
                    "error", e.getMessage()
            ));
        }
        
        return response;
    }

    @GetMapping("/")
    public String index(Model model) {
        ConfiguracionColegio config = configuracionRepository.findFirstByActivoTrue()
                .orElseGet(() -> new ConfiguracionColegio("Colegio por defecto", "Lema", "Dirección", "-", "-"));

        model.addAttribute("colegio", config);
        model.addAttribute("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        model.addAttribute("dbStatus", "Conectado a MySQL (asistencia_escolar_db)");
        return "index";
    }
}
