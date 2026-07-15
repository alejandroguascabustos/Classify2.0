@echo off
REM ============================================================
REM  Inicia el proyecto Classify 2.0 en Windows (100% Java):
REM    App principal (Spring Boot)  -> http://localhost:8090
REM  El modulo Programacion ahora es parte de la misma app (/programacion).
REM  Requisitos: Java 21 y PostgreSQL corriendo.
REM  Doble clic para ejecutar.
REM ============================================================
cd /d "%~dp0"

echo ── App principal (Spring Boot, puerto 8090) ──

REM Libera el puerto 8090 si esta ocupado
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8090 ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo Compilando (puede tardar 1-2 min)...
call mvnw.cmd clean package -DskipTests -q

set "JAR="
for %%j in (target\*.jar) do set "JAR=%%j"
if "%JAR%"=="" (
    echo ERROR: no se genero el .jar. Revisa la compilacion.
    pause
    exit /b 1
)

echo Arrancando %JAR%  -^>  http://localhost:8090/inicio
echo   Programacion: http://localhost:8090/programacion
java -jar "%JAR%" --server.port=8090
pause
