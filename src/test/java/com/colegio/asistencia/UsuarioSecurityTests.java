package com.colegio.asistencia;

import com.colegio.asistencia.model.EnumRol;
import com.colegio.asistencia.model.Usuario;
import com.colegio.asistencia.repository.UsuarioRepository;
import com.colegio.asistencia.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UsuarioSecurityTests {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Debe validar que el usuario admin existe con contraseña cifrada en BCrypt")
    void testUsuarioAdminYPasswordBCrypt() {
        Optional<Usuario> adminOpt = usuarioRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "El usuario 'admin' debe existir en MySQL");

        Usuario admin = adminOpt.get();
        assertTrue(admin.getActivo(), "El usuario admin debe estar activo");
        assertTrue(passwordEncoder.matches("admin123", admin.getPassword()), 
                "La contraseña cifrada debe coincidir con 'admin123'");
        assertFalse(admin.getPassword().equals("admin123"), 
                "La contraseña NUNCA debe guardarse en texto plano");
        assertTrue(admin.tieneRol(EnumRol.ROLE_ADMIN), 
                "El usuario admin debe poseer el rol ROLE_ADMIN");
    }

    @Test
    @DisplayName("Debe cargar UserDetails correctamente a través de CustomUserDetailsService")
    void testLoadUserByUsername() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");
        assertNotNull(userDetails, "UserDetails no debe ser nulo");
        assertEquals("admin", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Debe rechazar una contraseña incorrecta")
    void testRechazoPasswordInvalida() {
        Optional<Usuario> adminOpt = usuarioRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent());
        assertFalse(passwordEncoder.matches("password_erronea", adminOpt.get().getPassword()));
    }
}
