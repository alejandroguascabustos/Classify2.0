"""
Equivalente Python de com.classify20.service.AgendaService.

El repositorio JPA (AgendaRepository) no necesita equivalente:
el ORM de Django ya provee save() y all() (findAll). Para editar y eliminar
se usan get(), save() y delete(), equivalentes a findById/save/deleteById.
"""
from .models import Agenda


def guardar_agenda(agenda: Agenda) -> Agenda:
    """Guardar una nueva agenda (equivale a agendaRepository.save)."""
    agenda.save()
    return agenda


def listar_agendas():
    """Listar todas las agendas (equivale a agendaRepository.findAll)."""
    return Agenda.objects.all().order_by("id")


def obtener_agenda(agenda_id: int) -> Agenda:
    """1. Manejo crucial: ¿Qué pasa si el ID no existe?"""
    try:
        return Agenda.objects.get(pk=agenda_id)
    except Agenda.DoesNotExist:
        # Equivale al EntityNotFound de Spring Boot
        raise ValueError(f"La agenda con ID {agenda_id} no existe.")


def actualizar_agenda(agenda_id: int, datos: dict) -> Agenda:
    """2. Manejo crucial: ¿Qué pasa si el ID no existe OR los datos son inválidos?"""
    try:
        agenda = Agenda.objects.get(pk=agenda_id)
        
        for campo, valor in datos.items():
            setattr(agenda, campo, valor)
            
        agenda.full_clean()  # Fuerza a Django a validar los datos antes de guardar
        agenda.save()
        return agenda
        
    except Agenda.DoesNotExist:
        raise ValueError(f"No se puede actualizar. La agenda con ID {agenda_id} no existe.")
    except (ValidationError, IntegrityError) as e:
        # Captura datos corruptos, campos nulos o violaciones de restricciones
        raise ValueError(f"Error de validación en los datos proporcionados: {e}")


def eliminar_agenda(agenda_id: int) -> None:
    """Eliminar una agenda por su id (equivale a agendaRepository.deleteById)."""
    Agenda.objects.get(pk=agenda_id).delete()
