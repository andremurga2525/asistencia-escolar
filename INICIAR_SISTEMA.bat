@echo off
title Sistema de Asistencia Escolar - I.E.P. Internacional Jeshua
cls
echo ==============================================================================
echo           I.E.P. INTERNACIONAL JESHUA - TUMAN, CHICLAYO - PERU
echo                 SISTEMA INTEGRAL DE ASISTENCIA ESCOLAR
echo ==============================================================================
echo.

:: 1. Comprobar Java
echo [1/3] Verificando Java...
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] No se encontro Java instalado en esta computadora.
    echo Por favor descargue e instale Java 17 o 21 de: https://adoptium.net/
    echo.
    pause
    exit /b 1
)
echo [OK] Java detectado correctamente.
echo.

:: 2. Iniciar MySQL 8.4 en puerto 3307 si no esta activo
echo [2/3] Verificando Base de Datos MySQL (Puerto 3307)...
netstat -ano | findstr ":3307" | findstr "LISTENING" >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [INFO] Iniciando servidor MySQL 8.4 local...
    if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" (
        start "" /b "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --defaults-file="C:\ProgramData\MySQL\MySQL Server 8.4\my.ini" --console >nul 2>nul
        timeout /t 3 /nobreak >nul
    ) else (
        echo [AVISO] Asegurese de que MySQL este iniciado en el puerto 3307 o 3306.
    )
) else (
    echo [OK] Base de datos MySQL activa en el puerto 3307.
)
echo.

:: 3. Iniciar aplicativo web y abrir navegador
echo [3/3] Iniciando Servidor Web Escolar...
echo Por favor espere mientras carga el sistema...
echo.

start "" powershell -WindowStyle Hidden -Command "Start-Sleep -Seconds 6; Start-Process 'http://localhost:8080/login'"

if exist "asistencia-escolar.jar" (
    java -jar asistencia-escolar.jar
) else (
    echo [ERROR] No se encontro el archivo asistencia-escolar.jar en esta carpeta.
)

pause
