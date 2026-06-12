#!/bin/bash
# Diagnóstico profundo: ¿qué hay en el puerto 8080?
cd "$(dirname "$0")"
{
    echo "=== netstat LISTEN 8080 (todos los usuarios) ==="
    netstat -an | grep -i listen | grep 8080
    echo ""
    echo "=== netstat todo 8080 ==="
    netstat -an | grep 8080
    echo ""
    echo "=== lsof listeners TCP (usuario actual) ==="
    lsof -nP -iTCP -sTCP:LISTEN
    echo ""
    echo "=== curl -v localhost:8080 ==="
    curl -v -m 5 http://localhost:8080/ 2>&1 | head -20
} > diagnostico-8080.txt 2>&1
echo "Listo: diagnostico-8080.txt"
