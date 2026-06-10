#!/bin/bash
# Arranca Classify (Spring Boot) desde el .jar empaquetado en el puerto 8090.
# Nota: el puerto 8080 está ocupado por otra instancia de PostgreSQL del
# sistema (socket /tmp/.s.PGSQL.8080), por eso se usa el 8090.
cd "$(dirname "$0")"

PUERTO=8090
lsof -ti :$PUERTO | xargs kill -9 2>/dev/null
sleep 1

# Recompila SIEMPRE para incluir los últimos cambios (plantillas, código)
echo "Compilando (puede tardar 1-2 min)..."
./mvnw clean package -DskipTests -q
JAR=$(ls target/*.jar 2>/dev/null | grep -v '\.original' | head -1)
if [ -z "$JAR" ]; then
    echo "ERROR: no se generó el .jar. Revisa la compilación."
    exit 1
fi

echo "Arrancando $JAR → http://localhost:$PUERTO/inicio"
java -jar "$JAR" --server.port=$PUERTO
