@echo off
title Subir Proyecto a GitHub - I.E.P. Internacional Jeshua
cls
echo ==============================================================================
echo             SUBIDOR AUTOMATICO A GITHUB - ASISTENCIA ESCOLAR
echo ==============================================================================
echo.

where git >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [AVISO] Git no esta instalado o no se encuentra en el PATH.
    echo.
    echo Puede descargarlo e instalarlo gratis desde: https://git-scm.com/download/win
    echo Durante la instalacion, deje marcadas las opciones por defecto y vuelva a
    echo ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

echo [1/5] Configurando usuario Git local...
git config user.name "Alexander Murga"
git config user.email "murgaalexander2@gmail.com"

echo [2/5] Inicializando repositorio Git...
if not exist ".git" (
    git init
    git branch -M main
)

echo [3/5] Agregando archivos del proyecto...
git add .

echo [4/5] Creando commit oficial...
git commit -m "Sistema Completo de Asistencia Escolar - I.E.P. Internacional Jeshua (Tuman)"

echo.
echo ==============================================================================
echo [5/5] VINCULAR CON SU REPOSITORIO DE GITHUB
echo ==============================================================================
echo.
echo Pasos sencillos en GitHub:
echo 1. Ingrese a https://github.com/new con su cuenta (murgaalexander2@gmail.com)
echo 2. Cree un repositorio llamado: asistencia-escolar
echo 3. Copie el enlace HTTPS del repositorio (ej: https://github.com/alex160107/asistencia-escolar.git)
echo.

set /p REPO_URL=Pegue aqui la URL de su repositorio de GitHub: 

if not "%REPO_URL%"=="" (
    git remote remove origin 2>nul
    git remote add origin %REPO_URL%
    echo.
    echo Subiendo codigo a GitHub...
    git push -u origin main
    if %ERRORLEVEL% equ 0 (
        echo.
        echo ==============================================================================
        echo [EXITO] El proyecto se ha subido correctamente a su cuenta de GitHub!
        echo ==============================================================================
    ) else (
        echo.
        echo [NOTA] Si le pide credenciales o Token:
        echo En GitHub: Settings -> Developer Settings -> Personal access tokens (Tokens classic)
        echo Cree un token con permiso 'repo' y uselo como contrasena al hacer push.
    )
)

echo.
pause
