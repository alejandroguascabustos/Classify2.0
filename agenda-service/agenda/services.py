
from .models import Agenda


CAMPOS_REQUERIDOS = [
    "grado", "grupo", "profesor", "materia", "fecha",
    "hora_inicio", "duracion", "modalidad", "tema_principal",
    "objetivos", "dificultades", "materiales_basicos", "recursos_necesarios",
]


def _validar_campos_completos(datos: dict) -> None:
    """Control 1: todos los campos deben estar presentes y no vacíos."""
    faltantes = [campo for campo in CAMPOS_REQUERIDOS if datos.get(campo) in (None, "", [])]
    if faltantes:
        raise ValueError(f"Campos obligatorios incompletos: {', '.join(faltantes)}")


def _validar_tipos_numericos(datos: dict) -> None:
    """Control 2: grado y duracion deben ser enteros positivos."""
    for campo in ("grado", "duracion"):
        valor = datos.get(campo)
        if valor is not None:
            try:
                if int(valor) <= 0:
                    raise ValueError(f"'{campo}' debe ser un entero positivo.")
            except (TypeError, ValueError):
                raise ValueError(f"'{campo}' debe ser un número entero válido.")


def guardar_agenda(agenda: Agenda) -> Agenda:
    """Guardar una nueva agenda (equivale a agendaRepository.save)."""
    datos = {f: getattr(agenda, f) for f in CAMPOS_REQUERIDOS}
    _validar_campos_completos(datos)
    _validar_tipos_numericos(datos)
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
    _validar_campos_completos(datos)
    _validar_tipos_numericos(datos)
    agenda = Agenda.objects.get(pk=agenda_id)
    for campo, valor in datos.items():
        setattr(agenda, campo, valor)
    agenda.save()
    return agenda


def eliminar_agenda(agenda_id: int) -> None:
    """Eliminar una agenda por su id (equivale a agendaRepository.deleteById)."""
    Agenda.objects.get(pk=agenda_id).delete()
