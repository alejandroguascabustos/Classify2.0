"""Mismas rutas que los @PostMapping/@GetMapping del controller Java."""
from django.urls import path

from . import views

urlpatterns = [
    path("", views.inicio, name="inicio"),
    path("guardar-agenda", views.guardar_agenda, name="guardar_agenda"),
    path("programacion/exportarExcel", views.exportar_excel, name="exportar_excel"),
]
