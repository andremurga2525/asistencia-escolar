package com.colegio.asistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PanelSecurityAndRoleTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin debe tener acceso exitoso a /admin/dashboard (200 OK)")
    void testAdminAccesoDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "auxiliar", roles = {"AUXILIAR"})
    @DisplayName("Auxiliar NO debe poder entrar al dashboard de admin (Acceso Denegado 403)")
    void testAuxiliarBloqueadoEnDashboardAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "10234567", roles = {"PADRE"})
    @DisplayName("Padre de familia NO debe poder entrar al dashboard de admin (Acceso Denegado 403)")
    void testPadreBloqueadoEnDashboardAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "auxiliar", roles = {"AUXILIAR"})
    @DisplayName("Auxiliar debe tener acceso exitoso a /auxiliar/escaner (200 OK)")
    void testAuxiliarAccesoEscaner() throws Exception {
        mockMvc.perform(get("/auxiliar/escaner"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "10234567", roles = {"PADRE"})
    @DisplayName("Padre de familia debe tener acceso exitoso a su portal /padre/hijos (200 OK)")
    void testPadreAccesoPortalHijos() throws Exception {
        mockMvc.perform(get("/padre/hijos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "10234567", roles = {"PADRE"})
    @DisplayName("Padre de familia NO debe poder entrar al escáner de auxiliares (403 Forbidden)")
    void testPadreBloqueadoEnEscanerAuxiliar() throws Exception {
        mockMvc.perform(get("/auxiliar/escaner"))
                .andExpect(status().isForbidden());
    }
}
