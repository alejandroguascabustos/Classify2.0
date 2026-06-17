"""Rutas del módulo Programación (Python/Django).

Mantiene las mismas rutas que el controller Java original y añade las de
edición y eliminación que solo existen en este servicio:

    GET  /                                   -> listado (página del módulo)
    POST /guardar-agenda                     -> crear (igual que Java)
    GET  /programacion/exportarExcel         -> Excel (igual que Java)
    GET  /programacion/agenda/<id>           -> datos de una agenda (JSON)
    POST /programacion/editar/<id>           -> actualizar una agenda
    POST /programacion/eliminar/<id>         -> eliminar una agenda
"""
from django.urls import path

from . import views

urlpatterns = [
    path("", views.inicio, name="inicio"),
    path("guardar-agenda", views.guardar_agenda, name="guardar_agenda"),
    path("programacion/exportarExcel", views.exportar_excel, name="exportar_excel"),
    path("programacion/agenda/<int:agenda_id>", views.obtener_agenda, name="obtener_agenda"),
    path("programacion/editar/<int:agenda_id>", views.editar_agenda, name="editar_agenda"),
    path("programacion/eliminar/<int:agenda_id>", views.eliminar_agenda, name="eliminar_agenda"),
]
