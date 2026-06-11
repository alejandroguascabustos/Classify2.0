#!/bin/bash
# Inicia el servicio Django del módulo Agenda en el puerto 8081.
# Doble clic desde Finder o ejecútalo en Terminal.
cd "$(dirname "$0")"

if [ ! -d "venv" ]; then
    echo "Creando entorno virtual..."
    python3 -m venv venv
fi
source venv/bin/activate
pip install -q -r requirements.txt

echo ""
echo "Servicio Agenda (Python) → http://localhost:8081"
echo ""
python manage.py runserver 8081
