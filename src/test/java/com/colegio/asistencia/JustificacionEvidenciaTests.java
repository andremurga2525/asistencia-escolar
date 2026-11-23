package com.colegio.asistencia;

import com.colegio.asistencia.model.EnumEstadoJustificacion;
import com.colegio.asistencia.model.JustificacionInasistencia;
import com.colegio.asistencia.repository.JustificacionInasistenciaRepository;
import com.colegio.asistencia.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JustificacionEvidenciaTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JustificacionInasistenciaRepository justificacionRepository;

    @Test
    @DisplayName("Debe guardar un archivo PDF o imagen de evidencia correctamente en disco")
    void testAlmacenamientoArchivoEvidencia() {
        MockMultipartFile mockPdf = new MockMultipartFile(
                "archivo",
                "receta_medica.pdf",
                "application/pdf",
                "Contenido de prueba simulando constancia medica".getBytes()
        );

        String nombreGuardado = fileStorageService.almacenarArchivo(mockPdf);
        assertNotNull(nombreGuardado, "El nombre del archivo guardado no debe ser nulo");
        assertTrue(nombreGuardado.endsWith(".pdf"), "Debe conservar la extensión .pdf");
        assertTrue(nombreGuardado.startsWith("EVID-"), "Debe tener el prefijo oficial de evidencia");

        Resource recurso = fileStorageService.cargarComoRecurso(nombreGuardado);
        assertTrue(recurso.exists(), "El archivo debe existir en la carpeta uploads/justificaciones");
        assertTrue(recurso.isReadable(), "El archivo debe ser legible por el servidor");
        assertEquals("application/pdf", fileStorageService.determinarContentType(nombreGuardado));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("El Administrador debe poder ver la lista de justificaciones y el detalle de una justificación")
    void testAdminBandejaYDetalleJustificacion() throws Exception {
        mockMvc.perform(get("/admin/justificaciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/justificaciones/lista"))
                .andExpect(model().attributeExists("justificaciones"))
                .andExpect(model().attributeExists("totalPendientes"));

        List<JustificacionInasistencia> lista = justificacionRepository.findAll();
        assertFalse(lista.isEmpty(), "Debe existir al menos una justificación de prueba en la base de datos");

        JustificacionInasistencia primerJust = lista.get(0);

        mockMvc.perform(get("/admin/justificaciones/" + primerJust.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/justificaciones/detalle"))
                .andExpect(model().attributeExists("justificacion"))
                .andExpect(model().attributeExists("tieneArchivo"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("El Administrador debe poder visualizar la evidencia PDF mediante el endpoint seguro")
    void testAdminVisualizarEvidenciaPdf() throws Exception {
        List<JustificacionInasistencia> lista = justificacionRepository.findAll();
        assertFalse(lista.isEmpty());

        JustificacionInasistencia just = lista.get(0);

        mockMvc.perform(get("/admin/justificaciones/" + just.getId() + "/evidencia"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(username = "auxiliar", roles = {"AUXILIAR"})
    @DisplayName("Un auxiliar NO debe poder acceder a la bandeja administrativa de justificaciones (Error 403)")
    void testAuxiliarBloqueadoDeJustificacionesAdmin() throws Exception {
        mockMvc.perform(get("/admin/justificaciones"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "10234567", roles = {"PADRE"})
    @DisplayName("Un padre de familia NO debe poder acceder a la bandeja administrativa de justificaciones (Error 403)")
    void testPadreBloqueadoDeJustificacionesAdmin() throws Exception {
        mockMvc.perform(get("/admin/justificaciones"))
                .andExpect(status().isForbidden());
    }
}
