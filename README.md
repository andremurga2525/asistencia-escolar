# 🏫 Sistema Integrado de Control de Asistencia Escolar con Escaneo QR
### Institución Educativa Particular "Internacional Jeshua" (Tumán, Chiclayo, Lambayeque - Perú)

Sistema integral desarrollado para la automatización del control de asistencia, generación de credenciales con código QR, módulo de justificaciones médicas familiares dentro del marco legal de 48 horas, panel directivo con analítica de puntualidad en tiempo real y migración masiva de alumnos.

---

## 🚀 Tecnologías Utilizadas

- **Backend:** Java 21 LTS, Spring Boot 3.3.4
- **Seguridad:** Spring Security 6 (Autenticación basada en roles: `ADMIN`, `AUXILIAR`, `PADRE`, cifrado BCrypt)
- **Persistencia:** Spring Data JPA, Hibernate ORM, MySQL 8.4 Community Server
- **Frontend / Vistas:** Thymeleaf, Bootstrap 5.3.3, Bootstrap Icons 1.11.3, Chart.js 4.4.4
- **Lector de Códigos QR:** HTML5-QRCode con cámara web y sintetizador Web Audio API para alertas sonoras de puntualidad y tardanza
- **Pruebas:** JUnit 5, Mockito, Spring Boot Test, Spring Security Test (27 pruebas automatizadas)

---

## 📋 Módulos Principales

### 1. Panel Directivo y Sidebar Izquierdo (`/admin/dashboard`)
- Sidebar vertical institucional responsive anclado a la izquierda.
- Indicadores KPI en tiempo real: Asistencia de hoy, Puntuales, Tardanzas e Inasistencias.
- Gráficos interactivos de puntualidad con Chart.js.
- Auditoría en vivo de los últimos ingresos por puerta.

### 2. Lector QR con Cámara Web y Feedback Sonoro (`/auxiliar/escaner`)
- Escaneo continuo con la cámara web del dispositivo o ingreso manual de código QR.
- Evaluación automática según las reglas y tolerancias del horario escolar:
  - **Puntual:** Tono agudo y confirmación en verde.
  - **Tardanza:** Doble tono de advertencia y cálculo de minutos de retraso.
  - **Error o no matriculado:** Alerta grave sonora y visual.
- Respuestas en milisegundos mediante API REST AJAX (`/auxiliar/api/escanear`) sin recarga de página.

### 3. Portal de Padres y Justificaciones Médicas (`/padre/hijos`)
- Seguridad estricta: Cada padre solo tiene acceso exclusivo al historial de sus hijos a cargo.
- Registro de inasistencias y botón condicional de **"Justificar Falta"** activo únicamente durante las **primeras 48 horas** de la falta.
- Carga de evidencias multipart (certificados médicos, recetas, fotos o PDFs).
- Autorregistro público de padres en `/registro-padre` con su DNI como usuario.

### 4. Bandeja Directiva de Justificaciones (`/admin/justificaciones`)
- Contador en tiempo real de solicitudes pendientes en el sidebar.
- Visor integrado de archivos PDF e imágenes en el navegador.
- Aprobación o rechazo con observaciones del director, actualizando de inmediato el récord escolar.

### 5. Migración Masiva de Alumnos por CSV (`/admin/estudiantes/importar`)
- Importación en lote de nóminas de estudiantes desde Excel o CSV.
- Creación automática de alumnos, generación de código QR institucional (`JESHUA-XXXXXXX`), matrícula en el año 2026 y vinculación con apoderados.
- Descarga de plantilla oficial en `/admin/estudiantes/plantilla-csv`.

### 6. Gestión de Usuarios y Permisos (`/admin/usuarios`)
- Administración de auxiliares y personal de recepción.
- Asignación de roles y control de estado (activo/inactivo).

---

## 🔑 Credenciales de Acceso para Pruebas

| Rol | Usuario | Contraseña | Acceso / Funcionalidad |
|---|---|---|---|
| **Director / Admin** | `admin` | `admin123` | Control total, sidebar izquierdo, justificaciones y reportes |
| **Auxiliar de Puerta** | `auxiliar` | `auxiliar123` | Escáner QR con cámara web (`/auxiliar/escaner`) |
| **Padre de Familia 1** | `10234567` | `padre123` | Apoderado de Lucas y Mateo Quispe |
| **Madre de Familia 2** | `10234568` | `padre123` | Apoderada de Valeria Torres Silva |
| **Padre de Familia 3** | `10234569` | `padre123` | Apoderado de Sofía Paredes Díaz (Inicial) |

---

## 📦 Ejecución Portátil para la Exposición

El proyecto incluye la carpeta empaquetada `SISTEMA-JESHUA-PORTABLE` lista para llevar en USB y ejecutar en la computadora del profesor:
1. Haga doble clic en `IMPORTAR_BASE_DATOS.bat` para cargar los datos en MySQL.
2. Haga doble clic en `INICIAR_SISTEMA.bat` para arrancar el sistema y abrir el navegador en `http://localhost:8080/login`.

---

**Desarrollado por:** Alexander Murga  
**Correo de contacto:** murgaalexander2@gmail.com  
**Institución:** I.E.P. Internacional Jeshua (Tumán - Lambayeque)
