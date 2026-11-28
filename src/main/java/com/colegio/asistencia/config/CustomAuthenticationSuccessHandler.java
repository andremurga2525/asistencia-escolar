package com.colegio.asistencia.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                response.sendRedirect("/admin/dashboard");
                return;
            } else if ("ROLE_PROFESOR".equals(role) || "ROLE_TUTOR".equals(role)) {
                response.sendRedirect("/profesor/dashboard");
                return;
            } else if ("ROLE_AUXILIAR".equals(role)) {
                response.sendRedirect("/auxiliar/escaner");
                return;
            } else if ("ROLE_PADRE".equals(role)) {
                response.sendRedirect("/padre/hijos");
                return;
            }
        }

        response.sendRedirect("/");
    }
}
