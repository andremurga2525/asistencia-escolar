@echo off
title Restaurar Base de Datos - I.E.P. Internacional Jeshua
cls
echo ==============================================================================
echo      RESTAURADOR DE BASE DE DATOS - I.E.P. INTERNACIONAL JESHUA
echo ==============================================================================
echo.
echo Este asistente cargara todas las tablas, usuarios, estudiantes y asistencias
echo en el servidor MySQL local.
echo.

set DB_HOST=127.0.0.1
set DB_PORT=3307
set DB_USER=asistencia_user
set DB_PASS=Asistencia2026!

echo Conectando a MySQL en %DB_HOST%:%DB_PORT% con usuario '%DB_USER%'...

if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% asistencia_escolar < base_de_datos_asistencia_escolar.sql
) else (
    mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% asistencia_escolar < base_de_datos_asistencia_escolar.sql
)

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==============================================================================
    echo [EXITO] Base de datos 'asistencia_escolar' importada exitosamente.
    echo Ahora puede hacer doble clic en 'INICIAR_SISTEMA.bat' para abrir la aplicacion.
    echo ==============================================================================
) else (
    echo.
    echo [ERROR] Hubo un problema al conectar con MySQL.
)

echo.
pause
