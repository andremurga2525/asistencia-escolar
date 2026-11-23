@echo off
title Subir Proyecto a GitHub - I.E.P. Internacional Jeshua
cls
echo ==============================================================================
echo             SUBIDOR AUTOMATICO A GITHUB - ASISTENCIA ESCOLAR
echo ==============================================================================
echo.

cd /d "C:\Users\murga\OneDrive\Desktop\asistencia-escolar"
"C:\Program Files\Git\cmd\git.exe" push -u origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==============================================================================
    echo [EXITO] Proyecto subido exitosamente a GitHub!
    echo ==============================================================================
) else (
    echo.
    echo [AVISO] Si se abrio una ventanita de GitHub en tu navegador, dale clic
    echo en "Authorize" o "Sign in with your browser" para autorizar el envio.
)

echo.
pause
