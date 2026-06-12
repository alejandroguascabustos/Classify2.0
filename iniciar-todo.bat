@echo off
REM ============================================================
REM  Inicia TODO el proyecto Classify 2.0 en Windows:
REM    1. Modulo Programacion (Python/Django)  -> http://localhost:8081
REM    2. App principal (Spring Boot)          -> http://localhost:8090
REM  Requisitos: Java 21, Python 3 y PostgreSQL corriendo.
REM  Doble clic para ejecutar.
REM ============================================================
cd /d "%~dp0"

echo ── 1/2 Modulo Programacion (Django, puerto 8081) ──
start "Classify-Django" cmd /k "cd agenda-service && (if not exist venv python -m venv venv) && call venv\Scripts\activate.bat && pip install -q -r requirements.txt && python manage.py runserver 8081"

echo.
echo ── 2/2 App principal (Spring Boot, puerto 8090) ──

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
java -jar "%JAR%" --server.port=8090
pause