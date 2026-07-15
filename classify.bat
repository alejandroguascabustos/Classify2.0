@echo off
REM ============================================================
REM  CLASSIFY 2.0 - Script unico para Windows (100% Java)
REM  Equivale a los .command de macOS:
REM    iniciar-todo / reiniciar-springboot
REM    consultar-usuarios / diagnostico-8080 / hacer-push
REM  El modulo Programacion es parte de la app Java (/programacion).
REM  Requisitos: Java 21, PostgreSQL (BD classify), Git
REM ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"
title Classify 2.0

REM ---- Configuracion BD (usada por psql) --------
set "DB_USER=postgres"
set "DB_PASSWORD=123456789"
set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=classify"

:menu
cls
echo ==============================================
echo            CLASSIFY 2.0 - WINDOWS
echo ==============================================
echo  1. Iniciar / Reiniciar Spring Boot (recompila)
echo  2. Consultar usuarios de la BD
echo  3. Diagnostico de puertos (8080/8090)
echo  4. Commit + push a GitHub
echo  5. Salir
echo ==============================================
set /p OP="Elige una opcion (1-5): "
if "%OP%"=="1" goto springboot
if "%OP%"=="2" goto usuarios
if "%OP%"=="3" goto diagnostico
if "%OP%"=="4" goto push
if "%OP%"=="5" exit /b 0
goto menu

REM ------------------------------------------------------------
:springboot
echo.
echo -- Spring Boot (puerto 8090) --
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8090 ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 >nul

if exist target (
    rd /s /q target >nul 2>&1
    if exist target (
        echo Esperando a que OneDrive libere archivos en target...
        timeout /t 3 >nul
        rd /s /q target >nul 2>&1
    )
)

echo Compilando (puede tardar 1-2 min)...
call mvnw.cmd package -DskipTests -q
set "JAR="
for %%j in (target\*.jar) do set "JAR=%%j"
if "!JAR!"=="" (
    echo ERROR: no se genero el .jar. Revisa la compilacion.
    pause
    goto menu
)
echo Arrancando !JAR!  -^>  http://localhost:8090/inicio
echo   Programacion: http://localhost:8090/programacion
java -jar "!JAR!" --server.port=8090
pause
goto menu

REM ------------------------------------------------------------
:usuarios
echo.
call :buscar_psql
if "!PSQL!"=="" (
    echo ERROR: no se encontro psql. Instala PostgreSQL o agregalo al PATH.
    pause
    goto menu
)
set "PGPASSWORD=!DB_PASSWORD!"
(
  echo == psql: !PSQL! ==
  "!PSQL!" -h !DB_HOST! -p !DB_PORT! -U !DB_USER! -d !DB_NAME! -c "SELECT id, documento, correo, nombre_usuario, tipo_usuario FROM registro_usuarios ORDER BY id;"
  echo == tokens ==
  "!PSQL!" -h !DB_HOST! -p !DB_PORT! -U !DB_USER! -d !DB_NAME! -c "SELECT id, id_usuario, token, expira_en, usado, creado_en FROM olvido_contrasenia_tokens ORDER BY id DESC LIMIT 10;"
) > consulta-usuarios.txt 2>&1
set "PGPASSWORD="
echo Listo: consulta-usuarios.txt
type consulta-usuarios.txt
pause
goto menu

:buscar_psql
set "PSQL="
where psql >nul 2>&1 && set "PSQL=psql"
if "!PSQL!"=="" (
    for /d %%v in ("C:\Program Files\PostgreSQL\*") do (
        if exist "%%v\bin\psql.exe" set "PSQL=%%v\bin\psql.exe"
    )
)
exit /b 0

REM ------------------------------------------------------------
:diagnostico
echo.
(
  echo === Puertos en escucha 8080 / 8090 ===
  netstat -aon | findstr "LISTENING" | findstr ":8080 :8090"
  echo.
  echo === Todo el trafico en esos puertos ===
  netstat -aon | findstr ":8080 :8090"
  echo.
  echo === Prueba HTTP a cada servicio ===
  curl -s -m 5 -o NUL -w "8090 Spring Boot -> HTTP %%{http_code}\n" http://localhost:8090/inicio
  curl -s -m 5 -o NUL -w "8080 (otro)      -> HTTP %%{http_code}\n" http://localhost:8080/
) > diagnostico-puertos.txt 2>&1
echo Listo: diagnostico-puertos.txt
type diagnostico-puertos.txt
pause
goto menu

REM ------------------------------------------------------------
:push
echo.
echo -- Commit + push a GitHub (autor: cristobalbelcor) --
set /p MSG="Mensaje del commit: "
if "!MSG!"=="" set "MSG=Actualizacion del proyecto"
git add -A
set GIT_AUTHOR_NAME=cristobalbelcor
set GIT_AUTHOR_EMAIL=cristobalbelcor@gmail.com
set GIT_COMMITTER_NAME=cristobalbelcor
set GIT_COMMITTER_EMAIL=cristobalbelcor@gmail.com
git commit -m "!MSG!"
git push origin main
echo == RESULTADO: %ERRORLEVEL% ==
pause
goto menu
