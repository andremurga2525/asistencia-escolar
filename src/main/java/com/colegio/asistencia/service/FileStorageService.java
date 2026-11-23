package com.colegio.asistencia.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rutaAlmacenamiento;
    private final List<String> extensionesPermitidas = Arrays.asList(".pdf", ".png", ".jpg", ".jpeg");

    public FileStorageService() {
        this.rutaAlmacenamiento = Paths.get("uploads/justificaciones").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rutaAlmacenamiento);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar la carpeta de almacenamiento de evidencias", e);
        }
    }

    public String almacenarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        String nombreOriginal = StringUtils.cleanPath(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo");
        String extension = "";
        int i = nombreOriginal.lastIndexOf('.');
        if (i > 0) {
            extension = nombreOriginal.substring(i).toLowerCase();
        }

        if (!extensionesPermitidas.contains(extension)) {
            throw new IllegalArgumentException("Formato no permitido. Solo se aceptan archivos PDF, PNG o JPG.");
        }

        // Generar nombre de archivo único y seguro
        String nombreSeguro = "EVID-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6) + extension;

        try {
            Path destino = this.rutaAlmacenamiento.resolve(nombreSeguro);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return nombreSeguro;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo de evidencia: " + e.getMessage(), e);
        }
    }

    public Resource cargarComoRecurso(String nombreArchivo) {
        try {
            Path archivo = this.rutaAlmacenamiento.resolve(nombreArchivo).normalize();
            Resource recurso = new UrlResource(archivo.toUri());
            if (recurso.exists() && recurso.isReadable()) {
                return recurso;
            } else {
                throw new RuntimeException("Archivo no encontrado o no legible: " + nombreArchivo);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ruta de archivo inválida: " + nombreArchivo, e);
        }
    }

    public String determinarContentType(String nombreArchivo) {
        if (nombreArchivo == null) return "application/octet-stream";
        String lower = nombreArchivo.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
