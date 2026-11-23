package com.colegio.asistencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "apoderados")
public class Apoderado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El DNI del apoderado es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos numéricos")
    @Column(name = "dni", nullable = false, unique = true, length = 8)
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "parentesco", length = 30)
    private String parentesco; // PADRE, MADRE, APODERADO_LEGAL

    /**
     * Usuario de inicio de sesión vinculado al apoderado (username = DNI).
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    /**
     * Estudiantes a cargo del apoderado (permite 1, 2, 3 o más hijos).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "apoderados_estudiantes",
            joinColumns = @JoinColumn(name = "apoderado_id"),
            inverseJoinColumns = @JoinColumn(name = "estudiante_id")
    )
    private Set<Estudiante> estudiantes = new HashSet<>();

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Apoderado() {
        this.activo = true;
    }

    public Apoderado(String dni, String nombres, String apellidos, String parentesco, String telefono) {
        this();
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.parentesco = parentesco;
        this.telefono = telefono;
    }

    public void agregarEstudiante(Estudiante estudiante) {
        this.estudiantes.add(estudiante);
    }

    public void removerEstudiante(Estudiante estudiante) {
        this.estudiantes.remove(estudiante);
    }

    public boolean tieneHijoConDni(String dniHijo) {
        return this.estudiantes.stream().anyMatch(e -> e.getDni().equals(dniHijo));
    }

    public String getNombreCompleto() {
        return this.apellidos + ", " + this.nombres;
    }

    // Getters y Setters completos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Set<Estudiante> getEstudiantes() { return estudiantes; }
    public void setEstudiantes(Set<Estudiante> estudiantes) { this.estudiantes = estudiantes; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
