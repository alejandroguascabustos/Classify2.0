"""
Equivalente Python de com.classify20.service.AgendaService.

El repositorio JPA (AgendaRepository) no necesita equivalente:
el ORM de Django ya provee save() y all() (findAll).
"""
from .models import Agenda


def guardar_agenda(agenda: Agenda) -> Agenda:
    """Guardar una nueva agenda (equivale a agendaRepository.save)."""
    agenda.save()
    return agenda


def listar_agendas():
    """Listar todas las agendas (equivale a agendaRepository.findAll)."""
    return Agenda.objects.all().order_by("id")
