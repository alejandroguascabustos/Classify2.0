"""Rutas raíz: delega en las URLs del módulo agenda (mismas rutas que el controller Java)."""
from django.urls import include, path

urlpatterns = [
    path("", include("agenda.urls")),
]
