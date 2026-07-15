#!/bin/bash
# Inicia el proyecto Classify 2.0 (100% Java / Spring Boot):
#   1. PostgreSQL (si está instalado con Homebrew)
#   2. App principal Spring Boot  → http://localhost:8090
# El módulo Programación ahora es parte de la misma app Java (/programacion).
# Doble clic desde Finder o ejecútalo en Terminal.
cd "$(dirname "$0")"

echo "── 1/2 PostgreSQL ──────────────────────────────"
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
echo "── 2/2 App principal (Spring Boot, puerto 8090) ──"
echo ""
echo "  Inicio       : http://localhost:8090/inicio"
echo "  Login        : http://localhost:8090/login"
echo "  Programación : http://localhost:8090/programacion"
echo ""
./mvnw spring-boot:run
