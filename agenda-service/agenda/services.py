
from .models import Agenda





def guardar_agenda(agenda: Agenda) -> Agenda:

    """Guardar una nueva agenda (equivale a agendaRepository.save)."""

    agenda.save()

    return agenda





def listar_agendas():

    """Listar todas las agendas (equivale a agendaRepository.findAll)."""

    return Agenda.objects.all().order_by("id")





def obtener_agenda(agenda_id: int) -> Agenda:

    """Obtener una agenda por su id (equivale a agendaRepository.findById)."""

    return Agenda.objects.get(pk=agenda_id)





def actualizar_agenda(agenda_id: int, datos: dict) -> Agenda:

    """

    Actualizar los campos de una agenda existente y guardarla.

    Recibe la agenda ya construida con los nuevos datos; conserva el mismo id

    (equivale a cargar la entidad, setear campos y llamar a save()).

    """

    agenda = Agenda.objects.get(pk=agenda_id)

    for campo, valor in datos.items():

        setattr(agenda, campo, valor)

    agenda.save()

    return agenda





def eliminar_agenda(agenda_id: int) -> None:

    """Eliminar una agenda por su id (equivale a agendaRepository.deleteById)."""

    Agenda.objects.get(pk=agenda_id).delete() 

