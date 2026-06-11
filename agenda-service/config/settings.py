"""
Configuración del servicio Django del módulo Agenda (Classify 2.0).

Usa la MISMA base de datos PostgreSQL que la app Spring Boot.
La conexión se toma de variables de entorno con los mismos valores
por defecto que application.properties:

    jdbc:postgresql://localhost:5432/classify  (usuario: classify_app)

Variables de entorno soportadas:
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
"""
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

SECRET_KEY = os.environ.get(
    "DJANGO_SECRET_KEY",
    "dev-classify-agenda-service-cambiar-en-produccion",
)

DEBUG = os.environ.get("DJANGO_DEBUG", "true").lower() == "true"

ALLOWED_HOSTS = ["*"]

# Solo lo necesario: sin admin, sin auth. Este servicio únicamente
# expone los endpoints del módulo Programación.
# staticfiles: sirve los archivos estáticos propios (static/css/...).
INSTALLED_APPS = [
    "django.contrib.staticfiles",
    "agenda",
]

MIDDLEWARE = [
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [BASE_DIR / "templates"],
        "APP_DIRS": True,
        "OPTIONS": {"context_processors": []},
    },
]

WSGI_APPLICATION = "config.wsgi.application"

# ── Base de datos: la misma PostgreSQL de Spring Boot ──────────
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "HOST": os.environ.get("DB_HOST", "localhost"),
        "PORT": os.environ.get("DB_PORT", "5432"),
        "NAME": os.environ.get("DB_NAME", "classify"),
        "USER": os.environ.get("DB_USER", "classify_app"),
        "PASSWORD": os.environ.get("DB_PASSWORD", ""),
    }
}

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# ── Archivos estáticos propios del módulo (CSS) ────────────────
STATIC_URL = "static/"
STATICFILES_DIRS = [BASE_DIR / "static"]

LANGUAGE_CODE = "es-co"
TIME_ZONE = "America/Bogota"
USE_I18N = True
USE_TZ = False  # fecha/hora se guardan tal cual, igual que LocalDate/LocalTime en Java
