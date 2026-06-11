#!/bin/bash
# Inicia TODO el proyecto Classify 2.0:
#   1. PostgreSQL (si está instalado con Homebrew)
#   2. Módulo Agenda en Python/Django  → http://localhost:8081
#   3. App principal Spring Boot       → http://localhost:8080
# Doble clic desde Finder o ejecútalo en Terminal.
cd "$(dirname "$0")"

echo "── 1/3 PostgreSQL ──────────────────────────────"
if command -v brew >/dev/null 2>&1; then
    brew services start postgresql@16 2>/dev/null \
        || brew services start postgresql@15 2>/dev/null \
        || brew services start postgresql@14 2>/dev/null \
        || brew services start postgresql 2>/dev/null \
        || echo "Aviso: no se pudo iniciar PostgreSQL con brew (puede que ya esté corriendo)."
else
    echo "Aviso: Homebrew no encontrado. Asegúrate de que PostgreSQL esté corriendo."
fi

echo ""
echo "── 2/3 Módulo Agenda (Python/Django, puerto 8081) ──"
(
    cd agenda-service
    if [ ! -d "venv" ]; then
        echo "Creando entorno virtual..."
        python3 -m venv venv
    fi
    source venv/bin/activate
    pip install -q -r requirements.txt
    python manage.py runserver 8081
) &
DJANGO_PID=$!

# Al cerrar esta terminal, detener también el servicio Django
trap 'kill $DJANGO_PID 2>/dev/null' EXIT

echo ""
echo "── 3/3 App principal (Spring Boot, puerto 8080) ──"
echo ""
echo "  Inicio : http://localhost:8080/inicio"
echo "  Login  : http://localhost:8080/login"
echo "  Agenda Python : http://localhost:8081/"
echo ""
./mvnw spring-boot:run
