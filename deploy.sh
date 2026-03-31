#!/bin/bash
cd ~/classify2.0
git fetch origin main > /dev/null 2>&1
LOCAL=\$(git rev-parse HEAD)
REMOTE=\$(git rev-parse origin/main)

if [ \$LOCAL != \$REMOTE ]; then
    echo \"\$(date): Detectados cambios en GitHub. Actualizando...\" >> ~/classify2.0/deploy.log
    git pull origin main >> ~/classify2.0/deploy.log 2>&1
    ./mvnw clean package -DskipTests >> ~/classify2.0/deploy.log 2>&1
    pkill -f 'classify20' || true
    nohup java -jar target/classify20-0.0.1-SNAPSHOT.jar > ~/classify2.0/app.log 2>&1 &
    echo \"\$(date): Despliegue completado con éxito.\" >> ~/classify2.0/deploy.log
fi
