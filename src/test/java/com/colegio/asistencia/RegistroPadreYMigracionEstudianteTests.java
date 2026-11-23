package com.colegio.asistencia;

import com.colegio.asistencia.model.Apoderado;
import com.colegio.asistencia.model.Estudiante;
import com.colegio.asistencia.model.ResultadoImportacion;
import com.colegio.asistencia.repository.ApoderadoRepository;
import com.colegio.asistencia.repository.EstudianteRepository;
import com.colegio.asistencia.repository.UsuarioRepository;
import com.colegio.asistencia.service.EstudianteService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RegistroPadreYMigracionEstudianteTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EstudianteService estudianteService;

    @BeforeEach
    void limpiarDatosPrueba() {
        jdbcTemplate.execute("DELETE FROM apoderados_estudiantes WHERE estudiante_id IN (SELECT id FROM estudiantes WHERE dni IN ('71990001', '71990002'))");
        jdbcTemplate.execute("DELETE FROM registros_asistencia WHERE estudiante_id IN (SELECT id FROM estudiantes WHERE dni IN ('71990001', '71990002'))");
        jdbcTemplate.execute("DELETE FROM matriculas WHERE estudiante_id IN (SELECT id FROM estudiantes WHERE dni IN ('71990001', '71990002'))");
        jdbcTemplate.execute("DELETE FROM estudiantes WHERE dni IN ('71990001', '71990002')");
        jdbcTemplate.execute("DELETE FROM apoderados WHERE dni = '10998877'");
        jdbcTemplate.execute("DELETE FROM usuarios WHERE username = '10998877'");
    }

    @Test
    @DisplayName("El formulario de registro de padres debe ser público y accesible sin autenticación")
    void testFormularioRegistroPadrePublico() throws Exception {
        mockMvc.perform(get("/registro-padre"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro_padre"))
                .andExpect(model().attributeExists("colegio"));
    }

    @Test
    @DisplayName("Un padre debe poder registrarse con su DNI y sugerencia de fecha de nacimiento como clave")
    void testProcesoRegistroPadreConVinculacionHijo() throws Exception {
        String dniPadreNuevo = "10998877";

        mockMvc.perform(post("/registro-padre")
                        .with(csrf())
                        .param("dni", dniPadreNuevo)
                        .param("nombres", "Alejandro")
                        .param("apellidos", "Morales Chavez")
                        .param("parentesco", "PADRE")
                        .param("telefono", "974000111")
                        .param("email", "amorales@gmail.com")
                        .param("password", "15081985") // Fecha de nacimiento como clave
                        .param("confirmPassword", "15081985")
                        .param("dniHijo", "71234567")) // Hijo Lucas Quispe
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Verificar que el apoderado se creó en la BD
        Optional<Apoderado> apoderadoOpt = apoderadoRepository.findByDni(dniPadreNuevo);
        assertTrue(apoderadoOpt.isPresent(), "El apoderado debe haberse creado en la BD");
        assertEquals("Alejandro", apoderadoOpt.get().getNombres());

        // Verificar que el usuario de login fue creado con username = DNI
        assertTrue(usuarioRepository.existsByUsername(dniPadreNuevo), "El usuario para login debe existir");

        // Verificar que se vinculó al hijo indicado
        assertTrue(apoderadoOpt.get().tieneHijoConDni("71234567"), "Debe tener vinculado al estudiante con DNI 71234567");
    }

    @Test
    @DisplayName("La migración masiva de estudiantes por CSV debe procesar alumnos, generar QR y matricularlos")
    void testMigracionMasivaCsvEstudiantes() {
        String csvData = "DNI,APELLIDOS,NOMBRES,FECHA_NACIMIENTO,GENERO,NIVEL,GRADO,SECCION,TURNO,DNI_APODERADO\n"
                + "71990001,Montenegro Diaz,Alonso Gabriel,2016-06-10,M,PRIMARIA,1o Grado de Primaria,A,MANANA,10234567\n"
                + "71990002,Vargas Rios,Luciana Belen,2017-03-25,F,PRIMARIA,1o Grado de Primaria,A,MANANA,10234568\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        ResultadoImportacion resultado = estudianteService.importarDesdeCsv(inputStream);

        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalProcesados(), "Debe procesar 2 registros");
        assertTrue(resultado.getTotalCreados() >= 1, "Debe haber creado estudiantes nuevos");

        // Verificar que se guardó el estudiante en la BD y tiene código QR generado
        Optional<Estudiante> est1 = estudianteRepository.findByDni("71990001");
        assertTrue(est1.isPresent(), "El estudiante 71990001 debe existir en la BD");
        assertTrue(est1.get().getCodigoQr().startsWith("JESHUA-"), "Debe tener prefijo institucional JESHUA-");

        // Verificar que el apoderado 10234567 tiene vinculado al estudiante
        Optional<Apoderado> apo = apoderadoRepository.findByDni("10234567");
        assertTrue(apo.isPresent());
        assertTrue(apo.get().tieneHijoConDni("71990001"), "El apoderado debe haber sido vinculado al nuevo alumno");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("El Administrador debe poder descargar la plantilla CSV oficial de migración")
    void testDescargarPlantillaCsv() throws Exception {
        mockMvc.perform(get("/admin/estudiantes/plantilla-csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"plantilla_estudiantes_jeshua.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"));
    }
}
